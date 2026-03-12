import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

export interface AuthUser {
  userId: number;
  name: string;
  email: string;
  role: string;
  institutionId: number;
  token: string;
  tokenType?: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly API = '/api/auth';
  private readonly KEY = 'rm_user';

  constructor(private http: HttpClient) {}

  login(email: string, password: string): Observable<AuthUser> {
    return this.http.post<AuthUser>(`${this.API}/login`, { email, password }).pipe(
      tap(user => localStorage.setItem(this.KEY, JSON.stringify(user)))
    );
  }

  logout() { localStorage.removeItem(this.KEY); }

  isLoggedIn(): boolean {
    const u = this.getUser();
    return !!u?.token;
  }

  getUser(): AuthUser | null {
    const s = localStorage.getItem(this.KEY);
    if (!s) return null;
    try {
      return JSON.parse(s) as AuthUser;
    } catch {
      return null;
    }
  }
}
