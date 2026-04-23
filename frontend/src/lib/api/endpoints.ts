import { api } from "@/lib/api/client";
import {
  authResponseSchema,
  sessionDetailSchema,
  userMeSchema,
  workoutPlanSchema,
  type AuthResponse,
  type SessionDetail,
  type UserMe,
  type WorkoutPlan,
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

export async function fetchActiveWorkoutPlan(): Promise<WorkoutPlan | null> {
  const res = await api.request<WorkoutPlan>({
    path: "/api/workout-plans/active",
    schema: workoutPlanSchema,
  });
  return (res as WorkoutPlan | undefined) ?? null;
}

export async function fetchActiveSession(): Promise<SessionDetail | null> {
  const res = await api.request<SessionDetail>({
    path: "/api/sessions/active",
    schema: sessionDetailSchema,
  });
  return (res as SessionDetail | undefined) ?? null;
}

export async function startSession(workoutDayId?: string): Promise<SessionDetail> {
  return api.request({
    method: "POST",
    path: "/api/sessions/start",
    body: workoutDayId ? { workoutDayId } : {},
    schema: sessionDetailSchema,
  });
}
