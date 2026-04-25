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
    dailyKcalGoal: z.number().int().nullable().optional(),
    dailyProteinGGoal: z.number().int().nullable().optional(),
    dailyCarbsGGoal: z.number().int().nullable().optional(),
    dailyFatGGoal: z.number().int().nullable().optional(),
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

export const sessionSetSchema = z.object({
  id: z.string().uuid(),
  exerciseId: z.string().uuid().nullable(),
  exerciseNameTr: z.string().nullable(),
  exerciseNameEn: z.string().nullable(),
  setNumber: z.number().int(),
  repsDone: z.number().int(),
  weightKg: z.number().nullable().optional(),
  rpe: z.number().int().nullable().optional(),
  completed: z.boolean(),
  notes: z.string().nullable().optional(),
  newPr: z.boolean().nullable().optional(),
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
  sets: z.array(sessionSetSchema),
});

export const lastPerformanceSchema = z.object({
  sessionId: z.string().uuid(),
  startedAt: z.string(),
  endedAt: z.string().nullable(),
  sets: z.array(sessionSetSchema),
});

export type ApiError = z.infer<typeof apiErrorSchema>;
export type AuthResponse = z.infer<typeof authResponseSchema>;
export type UserMe = z.infer<typeof userMeSchema>;
export type WorkoutDayExercise = z.infer<typeof workoutDayExerciseSchema>;
export type WorkoutDay = z.infer<typeof workoutDaySchema>;
export type WorkoutPlan = z.infer<typeof workoutPlanSchema>;
export type SessionSet = z.infer<typeof sessionSetSchema>;
export type SessionDetail = z.infer<typeof sessionDetailSchema>;
export const exerciseSchema = z.object({
  id: z.string().uuid(),
  nameTr: z.string(),
  nameEn: z.string(),
  category: z.string(),
  equipment: z.string(),
  musclePrimary: z.string(),
  muscleSecondary: z.string().nullable().optional(),
  descriptionTr: z.string().nullable().optional(),
  descriptionEn: z.string().nullable().optional(),
  formTipsTr: z.array(z.string()),
  formTipsEn: z.array(z.string()),
  commonMistakesTr: z.array(z.string()),
  commonMistakesEn: z.array(z.string()),
  imageUrl: z.string().nullable().optional(),
  videoUrl: z.string().nullable().optional(),
  difficulty: z.string(),
});

export function pageSchema<T extends z.ZodTypeAny>(item: T) {
  return z.object({
    content: z.array(item),
    totalElements: z.number().int(),
    totalPages: z.number().int(),
    number: z.number().int(),
    size: z.number().int(),
  });
}

export const exercisePageSchema = pageSchema(exerciseSchema);

export const sessionSummarySchema = z.object({
  id: z.string().uuid(),
  workoutDayId: z.string().uuid().nullable(),
  startedAt: z.string(),
  endedAt: z.string().nullable(),
  finished: z.boolean(),
  setCount: z.number().int(),
  mood: z.number().int().nullable().optional(),
  energyLevel: z.number().int().nullable().optional(),
});

export const sessionSummaryPageSchema = pageSchema(sessionSummarySchema);

export const bodyMetricSchema = z.object({
  id: z.string().uuid(),
  recordedDate: z.string(),
  weightKg: z.number().nullable().optional(),
  bodyFatPercent: z.number().nullable().optional(),
  waistCm: z.number().nullable().optional(),
  chestCm: z.number().nullable().optional(),
  armCm: z.number().nullable().optional(),
  thighCm: z.number().nullable().optional(),
  photoUrl: z.string().nullable().optional(),
  notes: z.string().nullable().optional(),
  createdAt: z.string(),
  updatedAt: z.string(),
});

export const bodyMetricListSchema = z.array(bodyMetricSchema);

export const weeklyVolumeSchema = z.object({
  weekStart: z.string(),
  totalVolumeKg: z.number(),
  sessionCount: z.number().int(),
});
export const weeklyVolumeListSchema = z.array(weeklyVolumeSchema);

export const oneRmPointSchema = z.object({
  date: z.string(),
  estimatedOneRmKg: z.number(),
  repsDone: z.number().int(),
  weightKg: z.number().nullable(),
});
export const oneRmPointListSchema = z.array(oneRmPointSchema);

export const streakSchema = z.object({
  currentStreakDays: z.number().int(),
  longestStreakDays: z.number().int(),
  lastSessionDate: z.string().nullable(),
});

export const personalRecordSchema = z.object({
  exerciseId: z.string().uuid(),
  exerciseNameTr: z.string(),
  exerciseNameEn: z.string(),
  estimatedOneRmKg: z.number(),
  weightKg: z.number().nullable(),
  repsDone: z.number().int(),
  achievedAt: z.string(),
});
export const personalRecordListSchema = z.array(personalRecordSchema);

export const heatmapDaySchema = z.object({
  date: z.string(),
  sessionCount: z.number().int(),
});
export const heatmapListSchema = z.array(heatmapDaySchema);

export const supplementTimingValues = [
  "morning",
  "pre_workout",
  "post_workout",
  "evening",
  "with_meal",
  "other",
] as const;
export const supplementTimingSchema = z.enum(supplementTimingValues);

export const supplementSchema = z.object({
  id: z.string().uuid(),
  name: z.string(),
  dosage: z.string().nullable().optional(),
  timing: supplementTimingSchema,
  active: z.boolean(),
  reminderTime: z.string().nullable().optional(),
  createdAt: z.string(),
  updatedAt: z.string(),
});
export const supplementListSchema = z.array(supplementSchema);

export type SupplementTiming = z.infer<typeof supplementTimingSchema>;
export type Supplement = z.infer<typeof supplementSchema>;

export const foodItemSchema = z.object({
  id: z.string().uuid(),
  nameTr: z.string(),
  nameEn: z.string(),
  kcalPer100g: z.number(),
  proteinG: z.number(),
  carbsG: z.number(),
  fatG: z.number(),
  defaultServingG: z.number(),
});
export const foodItemListSchema = z.array(foodItemSchema);

export const nutritionEntrySchema = z.object({
  id: z.string().uuid(),
  foodId: z.string().uuid(),
  foodNameTr: z.string().nullable().optional(),
  foodNameEn: z.string().nullable().optional(),
  servingG: z.number(),
  kcal: z.number().nullable().optional(),
  proteinG: z.number().nullable().optional(),
  carbsG: z.number().nullable().optional(),
  fatG: z.number().nullable().optional(),
  consumedAt: z.string(),
  notes: z.string().nullable().optional(),
});
export const nutritionEntryListSchema = z.array(nutritionEntrySchema);

export type FoodItem = z.infer<typeof foodItemSchema>;
export type NutritionEntry = z.infer<typeof nutritionEntrySchema>;

export const waterEntrySchema = z.object({
  id: z.string().uuid(),
  ml: z.number().int(),
  consumedAt: z.string(),
});

export const waterDaySchema = z.object({
  date: z.string(),
  totalMl: z.number().int(),
  entries: z.array(waterEntrySchema),
});

export type WaterEntry = z.infer<typeof waterEntrySchema>;
export type WaterDay = z.infer<typeof waterDaySchema>;

export type WeeklyVolume = z.infer<typeof weeklyVolumeSchema>;
export type OneRmPoint = z.infer<typeof oneRmPointSchema>;
export type Streak = z.infer<typeof streakSchema>;
export type PersonalRecord = z.infer<typeof personalRecordSchema>;
export type HeatmapDay = z.infer<typeof heatmapDaySchema>;

export type Exercise = z.infer<typeof exerciseSchema>;
export type ExercisePage = z.infer<typeof exercisePageSchema>;
export type LastPerformance = z.infer<typeof lastPerformanceSchema>;
export type SessionSummary = z.infer<typeof sessionSummarySchema>;
export type SessionSummaryPage = z.infer<typeof sessionSummaryPageSchema>;
export type BodyMetric = z.infer<typeof bodyMetricSchema>;
