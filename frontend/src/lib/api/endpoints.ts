import { api } from "@/lib/api/client";
import {
  authResponseSchema,
  bodyMetricListSchema,
  bodyMetricSchema,
  exercisePageSchema,
  exerciseSchema,
  heatmapListSchema,
  lastPerformanceSchema,
  oneRmPointListSchema,
  personalRecordListSchema,
  sessionDetailSchema,
  sessionSetSchema,
  sessionSummaryPageSchema,
  streakSchema,
  userMeSchema,
  weeklyVolumeListSchema,
  workoutDaySchema,
  workoutPlanSchema,
  type AuthResponse,
  type BodyMetric,
  type Exercise,
  type ExercisePage,
  type HeatmapDay,
  type LastPerformance,
  type OneRmPoint,
  type PersonalRecord,
  type SessionDetail,
  type SessionSet,
  type SessionSummaryPage,
  type Streak,
  type UserMe,
  type WeeklyVolume,
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

export type UpdateProfilePayload = {
  displayName?: string;
  heightCm?: number;
  weightKg?: number;
  birthDate?: string;
  gender?: string;
  healthNotes?: string;
  goals?: string;
};

export async function updateMe(payload: UpdateProfilePayload): Promise<UserMe> {
  return api.request({
    method: "PUT",
    path: "/api/users/me",
    body: payload,
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

export async function fetchClaudeSummary(days = 30): Promise<unknown> {
  // Schema intentionally omitted: the summary is pass-through to Claude,
  // so we only need to surface whatever shape the backend produces.
  return api.request({
    path: "/api/export/claude-summary",
    query: { days },
  });
}

export type UpsertBodyMetricPayload = {
  recordedDate: string;
  weightKg?: number;
  bodyFatPercent?: number;
  waistCm?: number;
  chestCm?: number;
  armCm?: number;
  thighCm?: number;
  notes?: string;
};

export async function fetchMetrics(): Promise<BodyMetric[]> {
  return api.request({
    path: "/api/metrics",
    schema: bodyMetricListSchema,
  });
}

export async function upsertMetric(
  payload: UpsertBodyMetricPayload
): Promise<BodyMetric> {
  return api.request({
    method: "POST",
    path: "/api/metrics",
    body: payload,
    schema: bodyMetricSchema,
  });
}

export async function deleteMetric(id: string): Promise<void> {
  await api.request({
    method: "DELETE",
    path: `/api/metrics/${id}`,
  });
}

export async function fetchWeeklyVolume(weeks = 12): Promise<WeeklyVolume[]> {
  return api.request({
    path: "/api/analytics/volume",
    query: { weeks },
    schema: weeklyVolumeListSchema,
  });
}

export async function fetchOneRepMax(exerciseId: string): Promise<OneRmPoint[]> {
  return api.request({
    path: `/api/analytics/one-rm/${exerciseId}`,
    schema: oneRmPointListSchema,
  });
}

export async function fetchStreak(): Promise<Streak> {
  return api.request({
    path: "/api/analytics/streak",
    schema: streakSchema,
  });
}

export async function fetchPersonalRecords(): Promise<PersonalRecord[]> {
  return api.request({
    path: "/api/analytics/prs",
    schema: personalRecordListSchema,
  });
}

export async function fetchHeatmap(weeks = 12): Promise<HeatmapDay[]> {
  return api.request({
    path: "/api/analytics/heatmap",
    query: { weeks },
    schema: heatmapListSchema,
  });
}

export async function reorderDayExercises(
  planId: string,
  dayId: string,
  itemIdsInOrder: string[]
): Promise<WorkoutDay> {
  return api.request({
    method: "POST",
    path: `/api/workout-plans/${planId}/days/${dayId}/exercises/reorder`,
    body: { itemIdsInOrder },
    schema: workoutDaySchema,
  });
}
