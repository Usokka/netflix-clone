import { Play } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Movie } from "@/types";
import Link from "next/link";

interface MovieCardProps {
  movie: Movie;
}

export default function MovieCard({ movie }: MovieCardProps) {
  // NOUVEAU : On construit le lien dynamique
  const watchLink = movie.timestamp && movie.timestamp > 0 
    ? `/watch/${movie.id}?t=${movie.timestamp}` 
    : `/watch/${movie.id}`;

  return (
    <Card className="group relative overflow-hidden bg-zinc-900 border-none cursor-pointer transition-all duration-300 hover:scale-105 hover:z-10 aspect-video w-full">
      <CardContent className="p-0 w-full h-full relative">
        <img
          src={movie.thumbnailUrl}
          alt={movie.title}
          className="object-cover w-full h-full transition-opacity duration-300 group-hover:opacity-40"
        />

        <div className="absolute inset-0 flex flex-col justify-end p-4 opacity-0 group-hover:opacity-100 transition-opacity duration-300 bg-gradient-to-t from-black/80 via-black/40 to-transparent">
          <div className="flex items-center gap-3 mb-2">
            <Link href={watchLink}>
              <button className="flex items-center justify-center w-10 h-10 rounded-full bg-white text-black hover:bg-neutral-200 transition">
                <Play className="w-5 h-5 fill-current ml-0.5" />
              </button>
            </Link>
            <h3 className="font-bold text-sm text-white line-clamp-1">{movie.title}</h3>
          </div>
          <p className="text-xs text-gray-400">
            {Math.floor(movie.durationSeconds / 60)} min
          </p>
        </div>

        {/* NOUVEAU : La barre de progression rouge */}
        {movie.progressPercentage !== undefined && movie.progressPercentage > 0 && (
          <div className="absolute bottom-0 left-0 w-full h-1 bg-zinc-600/70 overflow-hidden z-20">
            <div 
              className="h-full bg-red-600 transition-all duration-300" 
              style={{ width: `${movie.progressPercentage}%` }}
            />
          </div>
        )}
      </CardContent>
    </Card>
  );
}