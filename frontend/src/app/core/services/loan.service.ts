import { Injectable } from '@angular/core';
import { Apollo, gql } from 'apollo-angular';
import { HttpClient } from '@angular/common/http';
import { catchError, Observable, map } from 'rxjs';

export interface BorrowerProfile {
  borrowerId: number;
  fullName: string;
  nationalId: string;
  gender: string;
  age: number;
  location: string;
  businessSector: string;
  monthlyIncome: number;
  collateralValue: number;
  creditScore?: number;
}

@Injectable({ providedIn: 'root' })
export class LoanService {
  private readonly BASE = '/api';
  private readonly GRAPHQL = '/graphql';

  constructor(private http: HttpClient, private apollo: Apollo) {}

  getLoans(): Observable<any[]> {
    return this.apollo.query<any>({
      query: gql`
        query AllLoans {
          loans {
            loanId
            borrowerId
            institutionId
            loanAmount
            interestRate
            tenureMonths
            disbursementDate
            status
            createdAt
          }
        }
      `
    }).pipe(
      map(result => result?.data?.loans ?? []),
      catchError(() => this.http.get<any[]>(`${this.BASE}/loans`))
    );
  }
  getBorrowerLookups(): Observable<{ sectors: string[]; locations: string[] }> {
    return this.http.get<{ sectors: string[]; locations: string[] }>(`${this.BASE}/borrowers/lookups`);
  }
  getMyBorrowerProfile(): Observable<BorrowerProfile> {
    return this.http.get<BorrowerProfile>(`${this.BASE}/borrowers/me/profile`);
  }
  getLoan(id: number): Observable<any> {
    return this.apollo.query<any>({
      query: gql`
        query GetLoan($id: ID!) {
          loan(id: $id) {
            loanId
            borrowerId
            institutionId
            loanAmount
            interestRate
            tenureMonths
            disbursementDate
            status
            createdAt
          }
        }
      `,
      variables: { id }
    }).pipe(
      map(result => result?.data?.loan ?? null),
      catchError(() => this.http.get<any>(`${this.BASE}/loans/${id}`))
    );
  }
  // Only keep the GraphQL+REST fallback version below
  createBorrower(borrower: any): Observable<any> {
    return this.apollo.mutate<any>({
      mutation: gql`
        mutation CreateBorrower($borrower: BorrowerInput!) {
          createBorrower(borrower: $borrower) {
            borrowerId
            userId
            institutionId
            fullName
            nationalId
            gender
            age
            location
            businessSector
            monthlyIncome
            collateralValue
            creditScore
            createdAt
          }
        }
      `,
      variables: { borrower }
    }).pipe(
      map(result => result?.data?.createBorrower ?? null),
      catchError(() => this.http.post<any>(`${this.BASE}/borrowers`, borrower))
    );
  }
  createLoan(loan: any): Observable<any> {
    const rawUser = localStorage.getItem('rm_user');
    const hasToken = !!rawUser;
    if (!hasToken) {
      return this.http.post<any>(`${this.BASE}/loans`, loan);
    }
    return this.apollo.mutate<any>({
      mutation: gql`
        mutation CreateLoan($loan: LoanInput!) {
          createLoan(loan: $loan) {
            loanId
            borrowerId
            institutionId
            loanAmount
            interestRate
            tenureMonths
            disbursementDate
            status
            createdAt
          }
        }
      `,
      variables: {
        loan: {
          borrowerId: loan.borrowerId,
          institutionId: loan.institutionId,
          loanAmount: loan.loanAmount,
          interestRate: loan.interestRate,
          tenureMonths: loan.tenureMonths,
          disbursementDate: loan.disbursementDate,
          status: loan.status
        }
      }
    }).pipe(
      map(result => {
        if ((result as any)?.errors?.length) {
          throw new Error((result as any).errors[0]?.message ?? 'GraphQL loan creation failed');
        }
        return result?.data?.createLoan;
      }),
      catchError(() => this.http.post<any>(`${this.BASE}/loans`, loan))
    );
  }
  // Only keep the GraphQL+REST fallback version below
  updateLoan(id: number, loan: any): Observable<any> {
    return this.apollo.mutate<any>({
      mutation: gql`
        mutation UpdateLoan($id: ID!, $loan: LoanInput!) {
          updateLoan(id: $id, loan: $loan) {
            loanId
            borrowerId
            institutionId
            loanAmount
            interestRate
            tenureMonths
            disbursementDate
            status
            createdAt
          }
        }
      `,
      variables: {
        id,
        loan: {
          borrowerId: loan.borrowerId,
          institutionId: loan.institutionId,
          loanAmount: loan.loanAmount,
          interestRate: loan.interestRate,
          tenureMonths: loan.tenureMonths,
          disbursementDate: loan.disbursementDate,
          status: loan.status
        }
      }
    }).pipe(
      map(result => result?.data?.updateLoan ?? null),
      catchError(() => this.http.put<any>(`${this.BASE}/loans/${id}`, loan))
    );
  }
  getRiskScoreById(id: number): Observable<any> {
    return this.apollo.query<any>({
      query: gql`
        query GetRiskScore($id: ID!) {
          riskScore(id: $id) {
            riskId
            loanId
            riskScore
            probabilityDefault
            riskGrade
            recommendedLimit
            modelVersion
            explanationJson
            createdAt
          }
        }
      `,
      variables: { id }
    }).pipe(
      map(result => result?.data?.riskScore ?? null),
      catchError(() => this.http.get<any>(`${this.BASE}/risk-scores/${id}`))
    );
  }

  calculateRiskScore(req: any): Observable<any> {
    return this.apollo.mutate<any>({
      mutation: gql`
        mutation CalculateRiskScore($input: RiskScoreInput!) {
          calculateRiskScore(input: $input) {
            riskId
            loanId
            riskScore
            probabilityDefault
            riskGrade
            recommendedLimit
            modelVersion
            explanationJson
            createdAt
          }
        }
      `,
      variables: { input: req }
    }).pipe(
      map(result => result?.data?.calculateRiskScore ?? null),
      catchError(() => this.http.post<any>(`${this.BASE}/risk-scores/calculate`, req))
    );
  }

  saveRiskScore(rs: any): Observable<any> {
    return this.apollo.mutate<any>({
      mutation: gql`
        mutation CreateRiskScore($riskScore: RiskScoreInput!) {
          createRiskScore(riskScore: $riskScore) {
            riskId
            loanId
            riskScore
            probabilityDefault
            riskGrade
            recommendedLimit
            modelVersion
            explanationJson
            createdAt
          }
        }
      `,
      variables: { riskScore: rs }
    }).pipe(
      map(result => result?.data?.createRiskScore ?? null),
      catchError(() => this.http.post<any>(`${this.BASE}/risk-scores`, rs))
    );
  }

  updateRiskScore(id: number, rs: any): Observable<any> {
    return this.apollo.mutate<any>({
      mutation: gql`
        mutation UpdateRiskScore($id: ID!, $riskScore: RiskScoreInput!) {
          updateRiskScore(id: $id, riskScore: $riskScore) {
            riskId
            loanId
            riskScore
            probabilityDefault
            riskGrade
            recommendedLimit
            modelVersion
            explanationJson
            createdAt
          }
        }
      `,
      variables: { id, riskScore: rs }
    }).pipe(
      map(result => result?.data?.updateRiskScore ?? null),
      catchError(() => this.http.put<any>(`${this.BASE}/risk-scores/${id}`, rs))
    );
  }

  searchBorrowers(query: string): Observable<BorrowerProfile[]> {
    // If query is empty, fetch all borrowers
    if (!query) {
      return this.apollo.query<any>({
        query: gql`
          query AllBorrowers {
            borrowers {
              borrowerId
              fullName
              nationalId
              gender
              age
              location
              businessSector
              monthlyIncome
              collateralValue
              creditScore
              createdAt
            }
          }
        `
      }).pipe(
        map(result => result?.data?.borrowers ?? []),
        catchError(() => this.http.get<BorrowerProfile[]>(`${this.BASE}/borrowers`))
      );
    }
    // If query is present, use REST fallback (GraphQL search not implemented)
    return this.http.get<BorrowerProfile[]>(`${this.BASE}/borrowers/search?q=${encodeURIComponent(query)}`);
  }

  updateCreditScore(borrowerId: number, creditScore: number): Observable<BorrowerProfile> {
    return this.apollo.mutate<any>({
      mutation: gql`
        mutation UpdateBorrowerCreditScore($id: ID!, $creditScore: Int!) {
          updateBorrower(id: $id, borrower: { creditScore: $creditScore }) {
            borrowerId
            fullName
            nationalId
            gender
            age
            location
            businessSector
            monthlyIncome
            collateralValue
            creditScore
            createdAt
          }
        }
      `,
      variables: { id: borrowerId, creditScore }
    }).pipe(
      map(result => result?.data?.updateBorrower ?? null),
      catchError(() => this.http.put<BorrowerProfile>(`${this.BASE}/borrowers/${borrowerId}/credit-score`, { creditScore }))
    );
  }

  getLoansByBorrower(borrowerId: number): Observable<any[]> {
    return this.apollo.query<any>({
      query: gql`
        query LoansByBorrower($borrowerId: ID!) {
          loansByBorrower(borrowerId: $borrowerId) {
            loanId
            borrowerId
            institutionId
            loanAmount
            interestRate
            tenureMonths
            disbursementDate
            status
            createdAt
          }
        }
      `,
      variables: { borrowerId }
    }).pipe(
      map(result => result?.data?.loansByBorrower ?? []),
      catchError(() => this.http.get<any[]>(`${this.BASE}/loans?borrowerId=${borrowerId}`))
    );
  }

  editBorrower(id: number, borrower: BorrowerProfile): Observable<BorrowerProfile> {
    return this.apollo.mutate<any>({
      mutation: gql`
        mutation UpdateBorrower($id: ID!, $borrower: BorrowerInput!) {
          updateBorrower(id: $id, borrower: $borrower) {
            borrowerId
            fullName
            nationalId
            gender
            age
            location
            businessSector
            monthlyIncome
            collateralValue
            creditScore
            createdAt
          }
        }
      `,
      variables: { id, borrower }
    }).pipe(
      map(result => result?.data?.updateBorrower ?? null),
      catchError(() => this.http.put<BorrowerProfile>(`${this.BASE}/borrowers/${id}`, borrower))
    );
  }
}
