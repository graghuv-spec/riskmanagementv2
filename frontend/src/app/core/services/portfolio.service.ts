// ...existing code...
import { Injectable } from '@angular/core';
import { Apollo, gql } from 'apollo-angular';
import { HttpClient } from '@angular/common/http';
import { catchError, forkJoin, map, Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class PortfolioService {
  private readonly BASE = '/api';
  private readonly GRAPHQL = '/graphql';

  constructor(private http: HttpClient, private apollo: Apollo) {}

  getLoans(): Observable<any[]> {
    return this.apollo.query<any>({
      query: gql`
        query DashboardLoans {
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
  getBorrowers(): Observable<any[]> {
    return this.apollo.query<any>({
      query: gql`
        query PortfolioBorrowers {
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
      catchError(() => this.http.get<any[]>(`${this.BASE}/borrowers`))
    );
  }

  getRiskScores(): Observable<any[]> {
    return this.apollo.query<any>({
      query: gql`
        query PortfolioRiskScores {
          riskScores {
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
      `
    }).pipe(
      map(result => result?.data?.riskScores ?? []),
      catchError(() => this.http.get<any[]>(`${this.BASE}/risk-scores`))
    );
  }

  getRepayments(): Observable<any[]> {
    return this.apollo.query<any>({
      query: gql`
        query PortfolioRepayments {
          repayments {
            repaymentId
            loanId
            dueDate
            paymentDate
            amountDue
            amountPaid
            daysPastDue
          }
        }
      `
    }).pipe(
      map(result => result?.data?.repayments ?? []),
      catchError(() => this.http.get<any[]>(`${this.BASE}/repayments`))
    );
  }

  getMetrics(): Observable<any[]> {
    return this.apollo.query<any>({
      query: gql`
        query PortfolioMetrics {
          portfolioMetrics {
            metricName
            metricValue
          }
        }
      `
    }).pipe(
      map(result => result?.data?.portfolioMetrics ?? []),
      catchError(() => this.http.get<any[]>(`${this.BASE}/portfolio-metrics`))
    );
  }

  getDashboardData(): Observable<any> {
    return forkJoin({
      loans: this.getLoans(),
      borrowers: this.getBorrowers(),
      riskScores: this.getRiskScores(),
      metrics: this.getMetrics()
    });
  }

  getPortfolioData(): Observable<any> {
    return forkJoin({
      loans: this.getLoans(),
      borrowers: this.getBorrowers(),
      riskScores: this.getRiskScores(),
      repayments: this.getRepayments()
    });
  }
}
