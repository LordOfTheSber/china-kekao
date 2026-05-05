export interface AuthResponse {
  accessToken: string;
  refreshToken: string;
  user: {
    id: string;
    email: string;
    role: "ROLE_USER" | "ROLE_ADMIN";
  };
}

export interface ApiError {
  status: number;
  code?: string;
  message: string;
}
