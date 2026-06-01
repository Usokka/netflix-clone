import MovieRow from "@/components/MovieRow";
import { Play, Info } from "lucide-react";
import { Movie } from "@/types";
import { fetchFromBackend } from "@/lib/api";
import Link from "next/link";

export default async function Home() {
  const [allMovies, actionMovies, sciFiMovies] = await Promise.all([
    fetchFromBackend<Movie[]>("/movies"),
    fetchFromBackend<Movie[]>("/movies/genre/1"), // Genre Action (ID 1 dans data.sql)
    fetchFromBackend<Movie[]>("/movies/genre/2"), // Genre Sci-Fi (ID 2 dans data.sql)
  ]);

  const heroMovie = allMovies[0];

  return (
    <main className="min-h-screen bg-[#141414] pb-24 overflow-x-hidden">
      <nav className="w-full h-16 bg-gradient-to-b from-black/90 to-transparent flex items-center px-4 md:px-12 fixed top-0 z-50">
        <h1 className="text-red-600 font-extrabold text-xl md:text-3xl tracking-wider cursor-pointer">
          NETFLIX
        </h1>
      </nav>

      <div className="relative h-[56vw] max-h-[85vh] w-full bg-zinc-900 flex items-end shadow-2xl">
        <div className="absolute inset-0 bg-gradient-to-t from-[#141414] via-[#141414]/20 to-black/40 z-10" />
        {heroMovie && (
          <img
            src={heroMovie.thumbnailUrl}
            alt={heroMovie.title}
            className="absolute inset-0 w-full h-full object-cover brightness-[65%] -z-0"
          />
        )}
        

        <div className="relative z-20 pl-4 md:pl-12 pb-[10vw] max-w-xl space-y-4">
          <h2 className="text-2xl md:text-6xl font-black text-white drop-shadow-md tracking-tight">
            {heroMovie?.title || "Mon Catalogue"}
          </h2>
          <p className="text-xs md:text-sm text-gray-300 font-medium drop-shadow-sm line-clamp-3 hidden sm:block">
            Découvre ce chef-d'œuvre exclusif disponible dès maintenant sur ta plateforme de streaming.
          </p>
          <div className="flex items-center gap-3 pt-2">
            <Link href={heroMovie ? `/watch/${heroMovie.id}` : "#"}>
              <button className="flex items-center gap-2 bg-white text-black px-4 md:px-7 py-1.5 md:py-2.5 rounded font-bold hover:bg-neutral-200 transition text-sm md:text-base shadow">
                <Play className="w-4 h-4 md:w-5 md:h-5 fill-current" /> Lecture
              </button>
            </Link>
            <button className="flex items-center gap-2 bg-zinc-500/60 text-white px-4 md:px-7 py-1.5 md:py-2.5 rounded font-bold hover:bg-zinc-500/40 transition text-sm md:text-base backdrop-blur-sm">
              <Info className="w-4 h-4 md:w-5 md:h-5" /> Plus d'infos
            </button>
          </div>
        </div>
      </div>

      <div className="relative z-30 -mt-12 md:-mt-24 space-y-4 md:space-y-12">
        <MovieRow title="Tendances Actuelles" movies={allMovies} />
        <MovieRow title="Films d'Action intenses" movies={actionMovies} />
        <MovieRow title="Incontournables Science-Fiction" movies={sciFiMovies} />
      </div>
    </main>
  );
}