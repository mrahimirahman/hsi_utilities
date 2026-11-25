import { HttpInterceptorFn } from '@angular/common/http';

export const sessionInterceptor: HttpInterceptorFn = (request, next) => {
  return next(request.clone({ withCredentials: true }));
};
