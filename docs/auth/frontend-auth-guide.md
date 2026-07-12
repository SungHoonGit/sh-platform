# 프론트엔드 인증 구현 가이드

## 개요

SH Platform 인증 시스템의 프론트엔드 구현을 위한 종합 가이드입니다.

## 1. 인증 시스템 개요

### 지원하는 인증 방식

| 방식 | 설명 | URL |
|------|------|-----|
| 이메일/비밀번호 | 일반 회원가입/로그인 | `/api/v1/auth/login` |
| 카카오 | 소셜 로그인 | `/oauth2/authorization/kakao` |
| 네이버 | 소셜 로그인 | `/oauth2/authorization/naver` |
| 구글 | 소셜 로그인 | `/oauth2/authorization/google` |
| 깃험 | 소셜 로그인 | `/oauth2/authorization/github` |

### 인증 흐름

```
1. 사용자가 로그인/회원가입 페이지 접속
2. 이메일/비밀번호 또는 소셜 로그인 선택
3. 소셜 로그인 선택 시 → 백엔드 OAuth2 URL로 리다이렉트
4. 백엔드에서 프로바이더 인증 페이지로 리다이렉트
5. 사용자가 프로바이더에서 인증
6. 프로바이더 → 백엔드 콜백
7. 백엔드에서 JWT 발급 → 프론트엔드 리다이렉트
8. 프론트엔드에서 JWT 저장
```

## 2. 화면 구현 가이드

### 2.1 로그인 화면

```tsx
// components/LoginForm.tsx
import { useState } from 'react';
import axios from 'axios';

const API_BASE = 'https://sunghoonyk.duckdns.org';

export function LoginForm() {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      const response = await axios.post(`${API_BASE}/api/v1/auth/login`, {
        email,
        password,
      });
      const { accessToken, refreshToken } = response.data.data;
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', refreshToken);
      window.location.href = '/dashboard';
    } catch (err: any) {
      setError(err.response?.data?.message || '로그인 실패');
    }
  };

  return (
    <form onSubmit={handleLogin}>
      <input
        type="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        placeholder="이메일"
      />
      <input
        type="password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        placeholder="비밀번호"
      />
      {error && <p className="error">{error}</p>}
      <button type="submit">로그인</button>
      
      {/* 소셜 로그인 버튼 */}
      <div className="social-login">
        <a href={`${API_BASE}/oauth2/authorization/kakao`}>
          카카오 로그인
        </a>
        <a href={`${API_BASE}/oauth2/authorization/naver`}>
          네이버 로그인
        </a>
        <a href={`${API_BASE}/oauth2/authorization/google`}>
          구글 로그인
        </a>
        <a href={`${API_BASE}/oauth2/authorization/github`}>
          깃험 로그인
        </a>
      </div>
    </form>
  );
}
```

### 2.2 OAuth2 콜백 페이지

```tsx
// pages/AuthCallback.tsx
import { useEffect } from 'react';
import { useSearchParams, useNavigate } from 'react-router-dom';

export function AuthCallback() {
  const [searchParams] = useSearchParams();
  const navigate = useNavigate();

  useEffect(() => {
    const accessToken = searchParams.get('accessToken');
    const refreshToken = searchParams.get('refreshToken');
    const provider = searchParams.get('provider');
    const returnUrl = searchParams.get('returnUrl') || '/';

    if (accessToken && refreshToken) {
      localStorage.setItem('accessToken', accessToken);
      localStorage.setItem('refreshToken', refreshToken);
      localStorage.setItem('provider', provider || '');
      navigate(returnUrl);
    } else {
      navigate('/auth/error?message=oauth2_failed');
    }
  }, [searchParams, navigate]);

  return <div>로그인 처리 중...</div>;
}
```

### 2.3 에러 페이지

```tsx
// pages/AuthError.tsx
import { useSearchParams } from 'react-router-dom';

export function AuthError() {
  const [searchParams] = useSearchParams();
  const message = searchParams.get('message') || 'unknown_error';

  const errorMessages: Record<string, string> = {
    oauth2_failed: '소셜 로그인에 실패했습니다.',
    email_not_found: '이메일 정보를 가져올 수 없습니다.',
    account_exists: '이미 가입된 이메일입니다.',
  };

  return (
    <div className="auth-error">
      <h2>로그인 오류</h2>
      <p>{errorMessages[message] || '알 수 없는 오류가 발생했습니다.'}</p>
      <a href="/login">다시 로그인</a>
    </div>
  );
}
```

## 3. API 호출 가이드

### 3.1 axios 설정

```tsx
// lib/api.ts
import axios from 'axios';

const API_BASE = 'https://sunghoonyk.duckdns.org';

export const api = axios.create({
  baseURL: API_BASE,
  headers: {
    'Content-Type': 'application/json',
  },
});

// 요청 인터셉터: JWT 자동 첨부
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('accessToken');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// 응답 인터셉터: 토큰 만료 처리
api.interceptors.response.use(
  (response) => response,
  async (error) => {
    if (error.response?.status === 401) {
      const refreshToken = localStorage.getItem('refreshToken');
      if (refreshToken) {
        try {
          const response = await axios.post(`${API_BASE}/api/v1/auth/refresh`, {
            refreshToken,
          });
          const { accessToken, refreshToken: newRefreshToken } = response.data.data;
          localStorage.setItem('accessToken', accessToken);
          localStorage.setItem('refreshToken', newRefreshToken);
          error.config.headers.Authorization = `Bearer ${accessToken}`;
          return axios(error.config);
        } catch {
          localStorage.clear();
          window.location.href = '/login';
        }
      }
    }
    return Promise.reject(error);
  }
);
```

### 3.2 API 호출 예제

```tsx
// 회원가입
const signup = async (email: string, password: string, name: string) => {
  const response = await api.post('/api/v1/auth/signup', { email, password, name });
  return response.data;
};

// 로그인
const login = async (email: string, password: string) => {
  const response = await api.post('/api/v1/auth/login', { email, password });
  return response.data;
};

// 토큰 갱신
const refresh = async (refreshToken: string) => {
  const response = await api.post('/api/v1/auth/refresh', { refreshToken });
  return response.data;
};

// 로그아웃
const logout = async (refreshToken: string) => {
  const response = await api.post('/api/v1/auth/logout', { refreshToken });
  return response.data;
};
```

## 4. 토큰 관리

### 4.1 JWT 구조

```json
{
  "sub": "1",
  "email": "user@example.com",
  "role": "USER",
  "iat": 1720666800,
  "exp": 1720670400
}
```

### 4.2 토큰 저장

```tsx
// localStorage에 저장
localStorage.setItem('accessToken', token);
localStorage.setItem('refreshToken', token);
localStorage.setItem('provider', 'kakao');

// 토큰 읽기
const accessToken = localStorage.getItem('accessToken');

// 토큰 삭제
localStorage.removeItem('accessToken');
localStorage.removeItem('refreshToken');
localStorage.removeItem('provider');
```

### 4.3 토큰 갱신 처리

```tsx
const isTokenExpired = (token: string): boolean => {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.exp * 1000 < Date.now();
  } catch {
    return true;
  }
};

if (isTokenExpired(accessToken)) {
  const newTokens = await refresh(refreshToken);
}
```

## 5. 에러 처리

### 5.1 에러 코드 목록

| 코드 | 상태 | 메시지 | 처리 |
|------|------|--------|------|
| UNAUTHORIZED | 401 | 이메일 또는 비밀번호가 올바르지 않습니다 | 로그인 폼 에러 표시 |
| EMAIL_NOT_VERIFIED | 403 | 이메일 인증이 완료되지 않았습니다 | 이메일 인증 안내 |
| DUPLICATE_EMAIL | 409 | 이미 사용 중인 이메일입니다 | 회원가입 폼 에러 표시 |
| RATE_LIMITED | 429 | 요청이 너무 많습니다 | 잠시 후 재시도 안내 |
| OAUTH2_FAILED | 401 | 소셜 로그인에 실패했습니다 | 로그인 페이지 리다이렉트 |
| TOKEN_EXPIRED | 401 | 토큰이 만료되었습니다 | 토큰 갱신 또는 재로그인 |
| PROVIDER_ALREADY_LINKED | 409 | 이미 연결된 프로바이더입니다 | 알림 표시 |
| PROVIDER_NOT_FOUND | 404 | 연결된 프로바이더를 찾을 수 없습니다 | 알림 표시 |
| LAST_PROVIDER_CANNOT_UNLINK | 400 | 마지막 프로바이더는 해제 불가 | 알림 표시 |

## 6. 계정 연결 (Account Linking)

### 6.1 개요

같은 이메일로 여러 프로바이더에서 로그인하면 하나의 계정으로 연결됩니다.

### 6.2 동작 흐름

```
1. 사용자가 카카오로 로그인 → 새 계정 생성
2. 같은 사람이 네이버로 로그인
3. 백엔드에서 이메일 확인 → 기존 계정 발견
4. 자동으로 네이버 프로바이더 연결
5. 같은 계정으로 로그인
```

### 6.3 연결된 프로바이더 확인

```tsx
const getLinkedProviders = async () => {
  const response = await api.get('/api/v1/auth/oauth2/providers');
  return response.data.data.providers;
};
```

### 6.4 프로바이더 연결 해제

```tsx
const unlinkProvider = async (provider: string) => {
  const response = await api.delete(`/api/v1/auth/oauth2/providers/${provider}`);
  return response.data;
};
```

## 7. 공통 컴포넌트

### 7.1 소셜 로그인 버튼

```tsx
// components/SocialLoginButtons.tsx
const API_BASE = 'https://sunghoonyk.duckdns.org';

export function SocialLoginButtons() {
  const providers = [
    { name: 'kakao', label: '카카오 로그인' },
    { name: 'naver', label: '네이버 로그인' },
    { name: 'google', label: '구글 로그인' },
    { name: 'github', label: '깃험 로그인' },
  ];

  return (
    <div className="social-login-buttons">
      {providers.map((provider) => (
        <a
          key={provider.name}
          href={`${API_BASE}/oauth2/authorization/${provider.name}`}
          className={`btn-social btn-${provider.name}`}
        >
          {provider.label}
        </a>
      ))}
    </div>
  );
}
```

## 8. 라우팅 설정

```tsx
// App.tsx
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import { LoginForm } from './components/LoginForm';
import { AuthCallback } from './pages/AuthCallback';
import { AuthError } from './pages/AuthError';

function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<LoginForm />} />
        <Route path="/auth/callback" element={<AuthCallback />} />
        <Route path="/auth/error" element={<AuthError />} />
      </Routes>
    </BrowserRouter>
  );
}
```

## 9. 환경 변수

```env
# .env
REACT_APP_API_BASE=https://sunghoonyk.duckdns.org
```

## 10. 주의사항

1. CORS: 백엔드에서 프론트엔드 도메인 허용 필요
2. HTTPS: 프로덕션에서는 반드시 HTTPS 사용
3. 토큰 보안: localStorage보다는 httpOnly 쿠키가 더 안전 (선택사항)
4. 에러 처리: 모든 API 호출에 에러 핸들링 필수
5. 로딩 상태: API 호출 중 로딩 UI 표시 필요
