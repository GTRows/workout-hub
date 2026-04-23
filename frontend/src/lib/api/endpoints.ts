import { api } from "@/lib/api/client";
import {
  authResponseSchema,
  exercisePageSchema,
  exerciseSchema,
  lastPerformanceSchema,
  sessionDetailSchema,
  sessionSetSchema,
  sessionSummaryPageSchema,
  userMeSchema,
  workoutDaySchema,
  workoutPlanSchema,
  type AuthResponse,
  type Exercise,
  type ExercisePage,
  type LastPerformance,
  type SessionDetail,
  type SessionSet,
  type SessionSummaryPage,
  type UserMe,
  type WorkoutDay,
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

export async function fetchWorkoutDay(dayId: string): Promise<WorkoutDay> {
  return api.request({
    path: `/api/workout-days/${dayId}`,
    schema: workoutDaySchema,
  });
}

export async function fetchActiveSession(): Promise<SessionDetail | null> {
  const res = await api.request<SessionDetail>({
    path: "/api/sessions/active",
    schema: sessionDetailSchema,
  });
  return (res as SessionDetail | undefined) ?? null;
}

export async function fetchSession(sessionId: string): Promise<SessionDetail> {
  return api.request({
    path: `/api/sessions/${sessionId}`,
    schema: sessionDetailSchema,
  });
}

export async function startSession(workoutDayId?: string): Promise<SessionDetail> {
  return api.request({
    method: "POST",
    path: "/api/sessions/start",
    body: workoutDayId ? { workoutDayId } : {},
    schema: sessionDetailSchema,
  });
}

export type AddSetPayload = {
  exerciseId: string;
  setNumber?: number;
  repsDone: number;
  weightKg?: number;
  rpe?: number;
  completed?: boolean;
  notes?: string;
};

export async function addSet(
  sessionId: string,
  payload: AddSetPayload
): Promise<SessionSet> {
  return api.request({
    method: "POST",
    path: `/api/sessions/${sessionId}/sets`,
    body: payload,
    schema: sessionSetSchema,
  });
}

export type FinishSessionPayload = {
  notes?: string;
  mood?: number;
  energyLevel?: number;
};

export async function finishSession(
  sessionId: string,
  payload: FinishSessionPayload = {}
): Promise<SessionDetail> {
  return api.request({
    method: "POST",
    path: `/api/sessions/${sessionId}/finish`,
    body: payload,
    schema: sessionDetailSchema,
  });
}

export async function fetchLastPerformance(
  exerciseId: string
): Promise<LastPerformance | null> {
  const res = await api.request<LastPerformance>({
    path: `/api/exercises/${exerciseId}/last-performance`,
    schema: lastPerformanceSchema,
  });
  return (res as LastPerformance | undefined) ?? null;
}

export type ExerciseListFilters = {
  category?: string;
  equipment?: string;
  difficulty?: string;
  page?: number;
  size?: number;
};

export async function fetchExercises(
  filters: ExerciseListFilters = {}
): Promise<ExercisePage> {
  return api.request({
    path: "/api/exercises",
    query: filters,
    schema: exercisePageSchema,
  });
}

export async function searchExercises(
  q: string,
  page = 0,
  size = 20
): Promise<ExercisePage> {
  return api.request({
    path: "/api/exercises/search",
    query: { q, page, size },
    schema: exercisePageSchema,
  });
}

export async function fetchExerciseDetail(id: string): Promise<Exercise> {
  return api.request({
    path: `/api/exercises/${id}`,
    schema: exerciseSchema,
  });
}

export async function fetchSessionHistory(
  page = 0,
  size = 200
): Promise<SessionSummaryPage> {
  return api.request({
    path: "/api/sessions/history",
    query: { page, size },
    schema: sessionSummaryPageSchema,
  });
}
