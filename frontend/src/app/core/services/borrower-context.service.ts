import { Injectable } from '@angular/core';
import { BehaviorSubject } from 'rxjs';
import { BorrowerProfile } from './loan.service';

@Injectable({ providedIn: 'root' })
export class BorrowerContextService {
  private readonly storageKey = 'rm_selected_borrower';
  private borrowerSubject = new BehaviorSubject<BorrowerProfile | null>(this.loadFromStorage());

  selectBorrower(borrower: BorrowerProfile) {
    sessionStorage.setItem(this.storageKey, JSON.stringify(borrower));
    this.borrowerSubject.next(borrower);
  }

  getSelectedBorrower(): BorrowerProfile | null {
    return this.borrowerSubject.value;
  }

  clearBorrower() {
    sessionStorage.removeItem(this.storageKey);
    this.borrowerSubject.next(null);
  }

  borrower$ = this.borrowerSubject.asObservable();

  private loadFromStorage(): BorrowerProfile | null {
    const raw = sessionStorage.getItem(this.storageKey);
    if (raw) {
      try {
        return JSON.parse(raw);
      } catch {
        return null;
      }
    }
    return null;
  }
}
