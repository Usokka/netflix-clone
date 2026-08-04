import MovieRow from "@/components/MovieRow";
import Navbar from "@/components/Navbar";
import { Play } from "lucide-react";
import { Movie, Genre } from "@/types";
import { ServerApiError, serverApiClient } from "@/lib/serverApiClient";
import Image from "next/image";
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
  const moviesByGenre: Record<number, Movie[]> = {};
  let watchlist: Movie[] = []; 
  let continueWatching: Movie[] = []; // NOUVEAU

  try {
    // NOUVEAU : Ajout de l'appel watch-history
    const [trendingRes, genresRes, watchlistRes, historyRes] = await Promise.all([
      serverApiClient.get<Movie[]>("/movies/trending"),
      serverApiClient.get<Genre[]>("/genres"),
      serverApiClient.get<Movie[]>("/watchlist"),
      serverApiClient.get<Movie[]>("/watch-history") 
    ]);

    trendingMovies = trendingRes;
    genres = genresRes;
    watchlist = watchlistRes;
    continueWatching = historyRes;

    const genrePromises = genres.map((genre) =>
      serverApiClient.get<Movie[]>(`/movies/genre/${genre.id}`)
        .then((movies) => ({ genreId: genre.id, movies }))
    );

    const genreResults = await Promise.all(genrePromises);
    
    genreResults.forEach((result) => {
      moviesByGenre[result.genreId] = result.movies;
    });

  } catch (error) {
    if (error instanceof ServerApiError && error.status === 401) redirect('/login');
    if (error instanceof ServerApiError && error.status === 403) redirect('/profiles');
    throw error;
  }

  // NOUVEAU : Si on a un film en cours, on peut le mettre en hero banner, sinon on prend les tendances
  const heroMovie = continueWatching.length > 0 ? continueWatching[0] : (trendingMovies.length > 0 ? trendingMovies[0] : null);
  
  // NOUVEAU : Lien de lecture intelligent pour la bannière
  const heroWatchLink = heroMovie 
    ? (heroMovie.timestamp ? `/watch/${heroMovie.id}?t=${heroMovie.timestamp}` : `/watch/${heroMovie.id}`)
    : "#";

  return (
    <main className="min-h-screen bg-[#141414] pb-24 overflow-x-hidden">
      <Navbar />

      <div className="relative h-[56vw] max-h-[85vh] w-full bg-zinc-900 flex items-end shadow-2xl mt-0">
        <div className="absolute inset-0 bg-gradient-to-t from-[#141414] via-[#141414]/20 to-black/40 z-10" />
        
        {heroMovie ? (
          <Image
            src={heroMovie.thumbnailUrl}
            alt={heroMovie.title}
            fill
            priority
            sizes="100vw"
            className="object-cover brightness-[65%]"
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
          
          {heroMovie && (
            <div className="flex items-center gap-3 pt-2">
            <Link
              href={heroWatchLink}
              className="flex items-center gap-2 bg-white text-black px-4 md:px-7 py-1.5 md:py-2.5 rounded font-bold hover:bg-neutral-200 transition text-sm md:text-base shadow"
            >
              <Play className="w-4 h-4 md:w-5 md:h-5 fill-current" /> {heroMovie?.timestamp ? "Reprendre" : "Lecture"}
            </Link>
            </div>
          )}
        </div>
      </div>

      <div className="relative z-30 -mt-12 md:-mt-24 space-y-4 md:space-y-12">
        
        {/* NOUVEAU : Rangée Reprendre la lecture */}
        {continueWatching.length > 0 && (
          <MovieRow title="Reprendre la lecture" movies={continueWatching} />
        )}

        {watchlist.length > 0 && (
          <div id="my-list">
            <MovieRow title="Ma Liste" movies={watchlist} />
          </div>
        )}

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
