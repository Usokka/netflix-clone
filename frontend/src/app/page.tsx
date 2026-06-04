import MovieRow from "@/components/MovieRow";
import Navbar from "@/components/Navbar";
import { Play, Info } from "lucide-react";
import { Movie, Genre } from "@/types";
import { serverApiClient } from "@/lib/serverApiClient";
import Link from "next/link";
import { cookies } from "next/headers";
import { redirect } from "next/navigation";

export default async function Home() {
  const cookieStore = await cookies();
  const profileId = cookieStore.get("profileId")?.value;

  if (!profileId) {
    redirect("/profiles");
  }

  let trendingMovies: Movie[] = [];
  let genres: Genre[] = [];
  let moviesByGenre: Record<number, Movie[]> = {};
  let watchlist: Movie[] = []; // <-- Nouvelle variable pour Ma Liste

  try {
    // On ajoute l'appel /watchlist dans notre exécution parallèle
    const [trendingRes, genresRes, watchlistRes] = await Promise.allSettled([
      serverApiClient.get<Movie[]>("/movies/trending"),
      serverApiClient.get<Genre[]>("/genres"),
      serverApiClient.get<Movie[]>("/watchlist")
    ]);

    trendingMovies = trendingRes.status === 'fulfilled' ? trendingRes.value : [];
    genres = genresRes.status === 'fulfilled' ? genresRes.value : [];
    watchlist = watchlistRes.status === 'fulfilled' ? watchlistRes.value : [];

    const genrePromises = genres.map((genre) =>
      serverApiClient.get<Movie[]>(`/movies/genre/${genre.id}`)
        .then((movies) => ({ genreId: genre.id, movies }))
        .catch(() => ({ genreId: genre.id, movies: [] }))
    );

    const genreResults = await Promise.all(genrePromises);
    
    genreResults.forEach((result) => {
      moviesByGenre[result.genreId] = result.movies;
    });

  } catch (error) {
    console.error("Erreur lors du chargement du catalogue:", error);
  }

  const heroMovie = trendingMovies.length > 0 ? trendingMovies[0] : null;

  return (
    <main className="min-h-screen bg-[#141414] pb-24 overflow-x-hidden">
      <Navbar />

      {/* HERO BANNER (Inchangé) */}
      <div className="relative h-[56vw] max-h-[85vh] w-full bg-zinc-900 flex items-end shadow-2xl mt-0">
        <div className="absolute inset-0 bg-gradient-to-t from-[#141414] via-[#141414]/20 to-black/40 z-10" />
        
        {heroMovie ? (
          <img
            src={heroMovie.thumbnailUrl}
            alt={heroMovie.title}
            className="absolute inset-0 w-full h-full object-cover brightness-[65%] -z-0"
          />
        ) : (
          <div className="absolute inset-0 w-full h-full bg-neutral-800" />
        )}

        <div className="relative z-20 pl-4 md:pl-12 pb-[10vw] max-w-xl space-y-4">
          <h2 className="text-3xl md:text-6xl font-black text-white drop-shadow-md tracking-tight">
            {heroMovie?.title || "Catalogue"}
          </h2>
          <p className="text-xs md:text-sm text-gray-300 font-medium drop-shadow-sm line-clamp-3 hidden sm:block">
            {heroMovie?.description || "Découvrez nos meilleurs films et séries disponibles dès maintenant sur votre plateforme de streaming."}
          </p>
          
          <div className="flex items-center gap-3 pt-2">
            <Link href={heroMovie ? `/watch/${heroMovie.id}` : "#"}>
              <button className="flex items-center gap-2 bg-white text-black px-4 md:px-7 py-1.5 md:py-2.5 rounded font-bold hover:bg-neutral-200 transition text-sm md:text-base shadow">
                <Play className="w-4 h-4 md:w-5 md:h-5 fill-current" /> Lecture
              </button>
            </Link>
            <button className="flex items-center gap-2 bg-zinc-500/60 text-white px-4 md:px-7 py-1.5 md:py-2.5 rounded font-bold hover:bg-zinc-500/40 transition text-sm md:text-base backdrop-blur-sm">
              <Info className="w-4 h-4 md:w-5 md:h-5" /> Plus d&apos;infos
            </button>
          </div>
        </div>
      </div>

      {/* GRILLE DES RANGÉES DE FILMS */}
      <div className="relative z-30 -mt-12 md:-mt-24 space-y-4 md:space-y-12">
        
        {/* NOUVEAU : Rangée Ma Liste (s'affiche uniquement si elle n'est pas vide) */}
        {watchlist.length > 0 && (
          <MovieRow title="Ma Liste" movies={watchlist} />
        )}

        {/* Rangée des tendances */}
        {trendingMovies.length > 0 && (
          <MovieRow title="Tendances Actuelles" movies={trendingMovies} />
        )}

        {genres.map((genre) => {
          const movies = moviesByGenre[genre.id];
          if (!movies || movies.length === 0) return null;

          return (
            <MovieRow 
              key={genre.id} 
              title={genre.name} 
              movies={movies} 
            />
          );
        })}
      </div>
    </main>
  );
}