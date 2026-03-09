import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { PortfolioService } from '../../core/services/portfolio.service';

@Component({
  selector: 'app-report-generator',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './report-generator.component.html',
  styleUrl: './report-generator.component.scss'
})
export class ReportGeneratorComponent implements OnInit {
  loans: any[] = [];
  riskScores: any[] = [];
  borrowers: any[] = [];
  loading = true;
  generating = '';

  constructor(private ps: PortfolioService) {}

  ngOnInit() {
    this.ps.getPortfolioData().subscribe({
      next: ({ loans, borrowers, riskScores }) => {
        this.loans = loans; this.riskScores = riskScores; this.borrowers = borrowers;
        this.loading = false;
      }
    });
  }

  get activeLoansCount(): number { return this.loans.filter(l => l.status === 'Active').length; }

  getBorrower(borrowerId: number): any {
    return this.borrowers.find(b => b.borrowerId === borrowerId) ?? {};
  }

  getRiskScore(loanId: number): any {
    return this.riskScores.find(r => r.loanId === loanId) ?? {};
  }

  async downloadPDF() {
    this.generating = 'pdf';
    const { jsPDF } = await import('jspdf');
    const doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });

    // Header
    doc.setFillColor(2, 132, 199);
    doc.rect(0, 0, 210, 30, 'F');
    doc.setTextColor(255,255,255);
    doc.setFontSize(18); doc.setFont('helvetica','bold');
    doc.text('RiskManagement Pro', 14, 12);
    doc.setFontSize(10); doc.setFont('helvetica','normal');
    doc.text('Portfolio Risk Report — ' + new Date().toLocaleDateString(), 14, 21);

    // Stats
    doc.setTextColor(15,23,42);
    doc.setFontSize(12); doc.setFont('helvetica','bold');
    doc.text('Portfolio Summary', 14, 42);
    doc.setFontSize(9); doc.setFont('helvetica','normal');
    const active = this.loans.filter(l => l.status === 'Active').length;
    const totalVal = this.loans.reduce((s, l) => s + (l.loanAmount||0), 0);
    const avgScore = this.riskScores.length ? (this.riskScores.reduce((s,r) => s + (r.riskScore||0), 0) / this.riskScores.length).toFixed(1) : 'N/A';
    doc.text(`Total Loans: ${this.loans.length}   Active: ${active}   Total Value: $${totalVal.toLocaleString()}   Avg Risk Score: ${avgScore}`, 14, 50);

    // Table header
    doc.setFillColor(240, 249, 255);
    doc.rect(14, 58, 182, 8, 'F');
    doc.setFont('helvetica','bold'); doc.setFontSize(8);
    doc.text('Loan ID', 16, 64); doc.text('Amount', 38, 64); doc.text('Status', 68, 64);
    doc.text('Borrower', 90, 64); doc.text('Risk Score', 140, 64); doc.text('Grade', 168, 64);

    // Table rows
    doc.setFont('helvetica','normal');
    let y = 72;
    this.loans.slice(0, 20).forEach((l, i) => {
      if (i % 2 === 0) { doc.setFillColor(248,250,252); doc.rect(14, y-5, 182, 7, 'F'); }
      const b = this.getBorrower(l.borrowerId);
      const rs = this.getRiskScore(l.loanId);
      doc.text(`#${l.loanId}`, 16, y);
      doc.text(`$${(l.loanAmount||0).toLocaleString()}`, 38, y);
      doc.text(l.status ?? '', 68, y);
      doc.text((b.fullName ?? '').substring(0, 18), 90, y);
      doc.text(rs.riskScore ? rs.riskScore.toFixed(0) : '-', 144, y);
      doc.text(rs.riskGrade ?? '-', 168, y);
      y += 7;
    });

    doc.save(`risk-report-${new Date().toISOString().split('T')[0]}.pdf`);
    this.generating = '';
  }

  downloadCSV() {
    this.generating = 'csv';
    const headers = ['Loan ID','Amount','Rate','Tenure','Status','Borrower','Sector','Risk Score','Grade','Probability of Default','Recommended Limit'];
    const rows = this.loans.map(l => {
      const b = this.getBorrower(l.borrowerId);
      const rs = this.getRiskScore(l.loanId);
      return [l.loanId, l.loanAmount, l.interestRate, l.tenureMonths, l.status,
        b.fullName ?? '', b.businessSector ?? '', rs.riskScore?.toFixed(1) ?? '',
        rs.riskGrade ?? '', rs.probabilityDefault?.toFixed(3) ?? '', rs.recommendedLimit?.toFixed(0) ?? ''];
    });
    const csv = [headers, ...rows].map(r => r.join(',')).join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url; a.download = `loans-${new Date().toISOString().split('T')[0]}.csv`;
    a.click(); URL.revokeObjectURL(url);
    this.generating = '';
  }

  async generateCreditMemo() {
    if (!this.loans.length) return;
    this.generating = 'memo';
    const { jsPDF } = await import('jspdf');
    const doc = new jsPDF({ orientation: 'portrait', unit: 'mm', format: 'a4' });
    const loan = this.loans[0];
    const b = this.getBorrower(loan.borrowerId);
    const rs = this.getRiskScore(loan.loanId);

    doc.setFillColor(2, 132, 199); doc.rect(0, 0, 210, 30, 'F');
    doc.setTextColor(255,255,255); doc.setFontSize(16); doc.setFont('helvetica','bold');
    doc.text('CREDIT MEMO', 14, 14);
    doc.setFontSize(9); doc.setFont('helvetica','normal');
    doc.text(`Date: ${new Date().toLocaleDateString()}   Ref: MEMO-${loan.loanId}`, 14, 22);

    doc.setTextColor(15,23,42); doc.setFontSize(12); doc.setFont('helvetica','bold');
    doc.text('Borrower Information', 14, 44);
    doc.setFontSize(9); doc.setFont('helvetica','normal');
    doc.text(`Name: ${b.fullName ?? 'N/A'}`, 14, 52);
    doc.text(`National ID: ${b.nationalId ?? 'N/A'}   Age: ${b.age ?? 'N/A'}   Gender: ${b.gender ?? 'N/A'}`, 14, 58);
    doc.text(`Location: ${b.location ?? 'N/A'}   Sector: ${b.businessSector ?? 'N/A'}`, 14, 64);
    doc.text(`Monthly Income: $${(b.monthlyIncome ?? 0).toLocaleString()}   Collateral: $${(b.collateralValue ?? 0).toLocaleString()}`, 14, 70);

    doc.setFontSize(12); doc.setFont('helvetica','bold');
    doc.text('Loan Details', 14, 84);
    doc.setFontSize(9); doc.setFont('helvetica','normal');
    doc.text(`Loan Amount: $${(loan.loanAmount ?? 0).toLocaleString()}   Rate: ${loan.interestRate}%   Tenure: ${loan.tenureMonths} months`, 14, 92);
    doc.text(`Status: ${loan.status}`, 14, 98);

    doc.setFontSize(12); doc.setFont('helvetica','bold');
    doc.text('Risk Assessment', 14, 112);
    doc.setFontSize(9); doc.setFont('helvetica','normal');
    doc.text(`Risk Score: ${rs.riskScore?.toFixed(0) ?? 'N/A'} / 100   Grade: ${rs.riskGrade ?? 'N/A'}`, 14, 120);
    doc.text(`Probability of Default: ${((rs.probabilityDefault ?? 0)*100).toFixed(1)}%   Recommended Limit: $${(rs.recommendedLimit ?? 0).toLocaleString()}`, 14, 126);
    doc.text(`Model: ${rs.modelVersion ?? 'N/A'}`, 14, 132);

    doc.setFontSize(12); doc.setFont('helvetica','bold');
    doc.text('Decision', 14, 146);
    const grade = rs.riskGrade ?? 'D';
    const decision = grade === 'A' || grade === 'B' ? '✓ APPROVED — Risk within acceptable thresholds.' : grade === 'C' ? '⚠ CONDITIONAL — Subject to additional collateral.' : '✗ DECLINED — Risk exceeds acceptable limits.';
    doc.setFontSize(10);
    doc.text(decision, 14, 154);

    doc.setFontSize(8); doc.setTextColor(100,116,139);
    doc.text('This credit memo was generated by RiskManagement Pro. For internal use only.', 14, 280);

    doc.save(`credit-memo-loan-${loan.loanId}.pdf`);
    this.generating = '';
  }
}
