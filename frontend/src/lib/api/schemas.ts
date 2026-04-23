import { z } from "zod";

export const apiErrorSchema = z.object({
  timestamp: z.string(),
  status: z.number().int(),
  error: z.string(),
  message: z.string(),
  path: z.string(),
  errors: z
    .array(z.object({ field: z.string(), message: z.string() }))
    .nullable()
    .optional(),
});

export const authResponseSchema = z.object({
  accessToken: z.string(),
  refreshToken: z.string(),
  userId: z.string().uuid(),
  role: z.enum(["USER", "ADMIN"]),
});

export const userMeSchema = z.object({
  id: z.string().uuid(),
  email: z.string().email(),
  displayName: z.string(),
  role: z.enum(["USER", "ADMIN"]),
  profile: z.object({
    heightCm: z.number().int().nullable().optional(),
    weightKg: z.number().nullable().optional(),
    birthDate: z.string().nullable().optional(),
    gender: z.string().nullable().optional(),
    healthNotes: z.string().nullable().optional(),
    goals: z.string().nullable().optional(),
  }),
});

export const workoutDayExerciseSchema = z.object({
  id: z.string().uuid(),
  exerciseId: z.string().uuid().nullable(),
  exerciseNameTr: z.string().nullable(),
  exerciseNameEn: z.string().nullable(),
  orderIndex: z.number().int(),
  targetSets: z.number().int(),
  targetRepsMin: z.number().int().nullable().optional(),
  targetRepsMax: z.number().int().nullable().optional(),
  targetWeightKg: z.number().nullable().optional(),
  restSeconds: z.number().int().nullable().optional(),
  notes: z.string().nullable().optional(),
});

export const workoutDaySchema = z.object({
  id: z.string().uuid(),
  dayOfWeek: z.number().int().min(1).max(7),
  name: z.string(),
  focus: z.string(),
  estimatedDurationMin: z.number().int().nullable().optional(),
  exercises: z.array(workoutDayExerciseSchema),
});

export const workoutPlanSchema = z.object({
  id: z.string().uuid(),
  name: z.string(),
  active: z.boolean(),
  createdAt: z.string(),
  updatedAt: z.string(),
  days: z.array(workoutDaySchema),
});

export const sessionDetailSchema = z.object({
  id: z.string().uuid(),
  workoutDayId: z.string().uuid().nullable(),
  startedAt: z.string(),
  endedAt: z.string().nullable(),
  notes: z.string().nullable().optional(),
  mood: z.number().int().nullable().optional(),
  energyLevel: z.number().int().nullable().optional(),
  finished: z.boolean(),
  sets: z.array(z.unknown()),
});

export type ApiError = z.infer<typeof apiErrorSchema>;
export type AuthResponse = z.infer<typeof authResponseSchema>;
export type UserMe = z.infer<typeof userMeSchema>;
export type WorkoutDayExercise = z.infer<typeof workoutDayExerciseSchema>;
export type WorkoutDay = z.infer<typeof workoutDaySchema>;
export type WorkoutPlan = z.infer<typeof workoutPlanSchema>;
export type SessionDetail = z.infer<typeof sessionDetailSchema>;
