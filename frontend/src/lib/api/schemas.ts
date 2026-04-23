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

export type ApiError = z.infer<typeof apiErrorSchema>;
export type AuthResponse = z.infer<typeof authResponseSchema>;
export type UserMe = z.infer<typeof userMeSchema>;
