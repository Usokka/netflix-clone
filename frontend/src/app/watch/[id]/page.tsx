// src/app/watch/[id]/page.tsx
import { ArrowLeft } from "lucide-react";
import Link from "next/link";
import { Movie } from "@/types";

interface WatchPageProps {
  params: Promise<{ id: string }>;
}

async function getMovieData(id: string): Promise<Movie> {
  const res = await fetch(`http://localhost:8080/api/v1/movies`, { cache: "no-store" });
  if (!res.ok) throw new Error("Failed to fetch movie");
  
  const movies: Movie[] = await res.json();
  const movie = movies.find((m) => m.id === id);
  if (!movie) throw new Error("Movie not found");
  
  return movie;
}

export default async function WatchPage({ params }: WatchPageProps) {
  const { id } = await params;
  const movie = await getMovieData(id);

  return (
    <main className="h-screen w-screen bg-black relative flex items-center justify-center">
      {/* Bouton Retour en haut à gauche */}
      <div className="absolute top-6 left-6 z-50 flex items-center gap-4 bg-black/40 p-2 rounded-full backdrop-blur-md group hover:bg-black/70 transition">
        <Link href="/" className="text-white flex items-center gap-2 font-semibold">
          <ArrowLeft className="w-6 h-6 transition group-hover:-translate-x-1" />
          <span className="opacity-0 group-hover:opacity-100 transition-opacity duration-300 pr-2">
            Retour à l'accueil
          </span>
        </Link>
      </div>

      {/* Titre éphémère du film en haut */}
      <div className="absolute top-8 left-1/2 -translate-x-1/2 z-40 text-center">
        <h1 className="text-xl md:text-2xl font-bold text-gray-400 opacity-60">
          Visionnage de : <span className="text-white opacity-100">{movie.title}</span>
        </h1>
      </div>

      {/* Zone du futur Lecteur Vidéo HLS C++ */}
      <div className="w-full h-full flex flex-col items-center justify-center border border-zinc-800 bg-zinc-950/20">
        <p className="text-zinc-500 font-mono text-sm tracking-widest animate-pulse">
          [ MOTEUR DE STREAMING NON CONNECTÉ ]
        </p>
        <p className="text-xs text-zinc-600 mt-2 max-w-md text-center font-sans">
          Le flux HLS cible sera récupéré depuis : <br />
          <code className="text-red-500/80">{movie.videoFolderUrl}</code>
        </p>
      </div>
    </main>
  );
}