import { ArrowLeft } from "lucide-react";
import Link from "next/link";
import { Movie } from "@/types";
import VideoPlayer from "@/components/VideoPlayer";
import { serverApiClient } from "@/lib/serverApiClient";

interface WatchPageProps {
  params: Promise<{ id: string }>;
  searchParams: Promise<{ t?: string }>; // NOUVEAU : On écoute le query param ?t=120
}

async function getMovieData(id: string): Promise<Movie> {
  try {
    const movie = await serverApiClient.get<Movie>(`/movies/${id}`);
    if (!movie) throw new Error("Movie not found");
    return movie;
  } catch (error) {
    console.error("Error fetching movie:", error);
    throw new Error(`Failed to fetch movie with ID: ${id}`);
  }
}

export default async function WatchPage({ params, searchParams }: WatchPageProps) {
  const { id } = await params;
  const { t } = await searchParams;
  const parsedTimestamp = t ? Number.parseInt(t, 10) : 0;

  const movie = await getMovieData(id);
  const timestamp = Number.isFinite(parsedTimestamp) && parsedTimestamp > 0
    ? Math.min(parsedTimestamp, movie.durationSeconds)
    : 0;

  return (
    <main className="h-screen w-screen bg-black relative flex items-center justify-center">
      
      <div className="absolute top-6 left-6 z-50 flex items-center gap-4 bg-black/40 p-2 rounded-full backdrop-blur-md group hover:bg-black/70 transition">
        <Link href="/" className="text-white flex items-center gap-2 font-semibold">
          <ArrowLeft className="w-6 h-6 transition group-hover:-translate-x-1" />
          <span className="opacity-0 group-hover:opacity-100 transition-opacity duration-300 pr-2">
            Retour à l&apos;accueil
          </span>
        </Link>
      </div>

      <div className="w-full h-full flex items-center justify-center bg-black">
        <div className="w-full max-w-5xl px-4 shadow-2xl shadow-black/80">
          {/* NOUVEAU : On passe le timestamp au lecteur */}
          <VideoPlayer 
            movieId={movie.id} 
            videoFolderUrl={movie.videoFolderUrl} 
            timestamp={timestamp} 
          />        
      </div>
      </div>

    </main>
  );
}
