import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly API = 'http://localhost:8080/api/auth';
  private readonly KEY = 'rm_user';

  constructor(private http: HttpClient) {}

  login(email: string, password: string): Observable<any> {
    return this.http.post<any>(`${this.API}/login`, { email, password }).pipe(
      tap(user => localStorage.setItem(this.KEY, JSON.stringify(user)))
    );
  }

  logout() { localStorage.removeItem(this.KEY); }

  isLoggedIn(): boolean { return !!localStorage.getItem(this.KEY); }

  getUser(): any {
    const s = localStorage.getItem(this.KEY);
    return s ? JSON.parse(s) : null;
  }
}
