import { api } from "@/lib/api/client";
import {
  authResponseSchema,
  userMeSchema,
  type AuthResponse,
  type UserMe,
} from "@/lib/api/schemas";

export async function login(email: string, password: string): Promise<AuthResponse> {
  return api.request({
    method: "POST",
    path: "/api/auth/login",
    body: { email, password },
    schema: authResponseSchema,
    auth: false,
  });
}

export async function fetchMe(): Promise<UserMe> {
  return api.request({
    method: "GET",
    path: "/api/users/me",
    schema: userMeSchema,
  });
}
