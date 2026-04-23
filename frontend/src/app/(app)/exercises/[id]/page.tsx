import { ExerciseDetailClient } from "./exercise-detail-client";

export default async function Page({
  params,
}: {
  params: Promise<{ id: string }>;
}) {
  const { id } = await params;
  return <ExerciseDetailClient id={id} />;
}
