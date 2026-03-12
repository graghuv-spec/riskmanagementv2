import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss'
})
export class LoginComponent {
  email = '';
  password = '';
  error = '';
  loading = false;
  private returnUrl = '/dashboard';

  constructor(private auth: AuthService, private route: ActivatedRoute, private router: Router) {
    this.returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/dashboard';

    if (this.route.snapshot.queryParamMap.get('reason') === 'session-expired') {
      this.error = 'Your session expired. Please sign in again.';
    }

    if (this.auth.isLoggedIn()) this.router.navigate(['/dashboard']);
  }

  login() {
    if (!this.email || !this.password) { this.error = 'Please enter email and password'; return; }
    this.error = ''; this.loading = true;
    this.auth.login(this.email, this.password).subscribe({
      next: () => this.router.navigateByUrl(this.returnUrl),
      error: () => { this.error = 'Invalid email or password.'; this.loading = false; }
    });
  }
}
