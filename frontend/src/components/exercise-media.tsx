"use client";

import { useEffect, useRef, useState } from "react";

type Props = {
  videoUrl?: string | null;
  imageUrl?: string | null;
  alt: string;
};

export function ExerciseMedia({ videoUrl, imageUrl, alt }: Props) {
  const ref = useRef<HTMLDivElement>(null);
  const [visible, setVisible] = useState(false);

  useEffect(() => {
    const el = ref.current;
    if (!el) return;
    if (typeof IntersectionObserver === "undefined") {
      // Fallback when the platform IntersectionObserver is unavailable
      // (test envs, older browsers). External-sync from a platform-feature
      // probe; eslint-plugin-react-hooks@7's set-state-in-effect rule does
      // not model this pattern.
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setVisible(true);
      return;
    }
    const obs = new IntersectionObserver(
      (entries) => {
        for (const entry of entries) {
          if (entry.isIntersecting) {
            setVisible(true);
            obs.disconnect();
            break;
          }
        }
      },
      { rootMargin: "200px" }
    );
    obs.observe(el);
    return () => obs.disconnect();
  }, []);

  if (!videoUrl && !imageUrl) return null;

  return (
    <div
      ref={ref}
      data-testid="exercise-media"
      className="overflow-hidden rounded-md border border-border"
    >
      {visible && videoUrl && (
        <video
          data-testid="exercise-media-video"
          src={videoUrl}
          title={alt}
          aria-label={alt}
          controls
          preload="metadata"
          className="block aspect-video w-full bg-muted"
        />
      )}
      {visible && !videoUrl && imageUrl && (
        // eslint-disable-next-line @next/next/no-img-element
        <img
          data-testid="exercise-media-image"
          src={imageUrl}
          alt={alt}
          title={alt}
          loading="lazy"
          className="block aspect-video w-full bg-muted object-cover"
        />
      )}
      {!visible && (
        <div
          aria-hidden="true"
          className="flex aspect-video w-full items-center justify-center bg-muted text-xs text-muted-foreground"
        >
          ...
        </div>
      )}
    </div>
  );
}
