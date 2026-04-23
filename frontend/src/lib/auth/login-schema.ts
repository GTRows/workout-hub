import { z } from "zod";

export const loginFormSchema = z.object({
  email: z.string().min(1, { message: "required" }).email({ message: "invalidEmail" }),
  password: z.string().min(1, { message: "required" }),
});

export type LoginFormValues = z.infer<typeof loginFormSchema>;
