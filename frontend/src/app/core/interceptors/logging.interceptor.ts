import { HttpInterceptorFn, HttpResponse } from '@angular/common/http';
import { tap } from 'rxjs/operators';

export const loggingInterceptor: HttpInterceptorFn = (req, next) => {
  const started = Date.now();
  const method = req.method;
  const url = req.url;

  console.log(`[HTTP →] ${method} ${url}`, req.body ? { body: req.body } : '');

  return next(req).pipe(
    tap({
      next: (event) => {
        if (event instanceof HttpResponse) {
          const elapsed = Date.now() - started;
          console.log(
            `[HTTP ←] ${method} ${url} — ${event.status} (${elapsed}ms)`,
            { body: event.body }
          );
        }
      },
      error: (err) => {
        const elapsed = Date.now() - started;
        console.error(
          `[HTTP ✗] ${method} ${url} — ${err.status ?? 'NETWORK'} (${elapsed}ms)`,
          { error: err.error ?? err.message }
        );
      }
    })
  );
};
