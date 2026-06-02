import { ArrowLeft } from "lucide-react";
import Link from "next/link";
import { Movie } from "@/types";
import VideoPlayer from "@/components/VideoPlayer";
import { fetchFromBackend } from "@/lib/api";

interface WatchPageProps {
  params: Promise<{ id: string }>;
}

async function getMovieData(id: string): Promise<Movie> {
  try {
    const movies = await fetchFromBackend<Movie[]>("/movies");
    const movie = movies.find((m) => m.id === id);
    if (!movie) throw new Error("Movie not found");
    return movie;
  } catch (error) {
    console.error("Error fetching movie:", error);
    throw new Error(`Failed to fetch movie with ID: ${id}`);
  }
}

export default async function WatchPage({ params }: WatchPageProps) {
  const { id } = await params;
  const movie = await getMovieData(id);

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

      <div className="absolute top-8 left-1/2 -translate-x-1/2 z-40 text-center pointer-events-none">
        <h1 className="text-xl md:text-2xl font-bold text-gray-400 opacity-60">
          Visionnage de : <span className="text-white opacity-100">{movie.title}</span>
        </h1>
      </div>

      <div className="w-full h-full flex items-center justify-center bg-black">
        <div className="w-full max-w-5xl px-4 shadow-2xl shadow-black/80">
          <VideoPlayer movieId={movie.videoFolderUrl} />
        </div>
      </div>

    </main>
  );
}