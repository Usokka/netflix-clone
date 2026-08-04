import Navbar from "@/components/Navbar";
import MovieCard from "@/components/MovieCard";
import { Movie, Genre } from "@/types";
import { ServerApiError, serverApiClient } from "@/lib/serverApiClient";
import { SearchX } from "lucide-react";
import Link from "next/link";
import { redirect } from "next/navigation";

interface SearchPageProps {
  searchParams: Promise<{ q?: string; genre?: string }>;
}

export default async function SearchPage({ searchParams }: SearchPageProps) {
  const { q, genre } = await searchParams;
  const query = q || "";
  const parsedGenreId = genre ? Number.parseInt(genre, 10) : Number.NaN;
  const currentGenreId = Number.isInteger(parsedGenreId) && parsedGenreId > 0 ? parsedGenreId : null;
  
  let movies: Movie[] = [];
  let allGenres: Genre[] = [];
  
  const apiParameters = new URLSearchParams();
  if (query) apiParameters.set('q', query);
  if (currentGenreId) apiParameters.set('genre', currentGenreId.toString());
  const apiUrl = `/movies/search?${apiParameters.toString()}`;

  try {
    // Exécution en parallèle : on récupère les films ET tous les genres dispos
    const [moviesRes, genresRes] = await Promise.all([
      (query || currentGenreId) ? serverApiClient.get<Movie[]>(apiUrl) : Promise.resolve([]),
      serverApiClient.get<Genre[]>("/genres")
    ]);

    movies = moviesRes;
    allGenres = genresRes;
  } catch (error) {
    if (error instanceof ServerApiError && error.status === 401) redirect('/login');
    if (error instanceof ServerApiError && error.status === 403) redirect('/profiles');
    throw error;
  }

  const buildGenreLink = (genreId: number) => {
    const parameters = new URLSearchParams();
    if (query) parameters.set('q', query);
    if (currentGenreId !== genreId) parameters.set('genre', genreId.toString());
    const search = parameters.toString();
    return search ? `/search?${search}` : '/search';
  };

  return (
    <main className="min-h-screen bg-[#141414] text-white pt-24 pb-12 overflow-x-hidden">
      <Navbar />
      
      <div className="px-4 md:px-12 max-w-7xl mx-auto">
        
        {/* EN-TÊTE DE RECHERCHE */}
        <div className="mb-8">
          {query ? (
            <h1 className="text-xl md:text-2xl text-gray-400 font-medium mb-4">
              Résultats pour <span className="text-white font-bold">&ldquo;{query}&rdquo;</span>
            </h1>
          ) : (
            <h1 className="text-xl md:text-2xl text-white font-bold mb-4">
              Explorer par genres
            </h1>
          )}

          {/* NOUVEAU : FILTRES PAR GENRES */}
          <div className="flex flex-wrap gap-2">
            {allGenres.map((g) => {
              const isActive = currentGenreId === g.id;
              return (
                <Link
                  key={g.id}
                  href={buildGenreLink(g.id)}
                  className={`px-4 py-1.5 rounded-full text-sm font-medium transition-all duration-300 border ${
                    isActive 
                      ? "bg-white text-black border-white" 
                      : "bg-zinc-900/80 text-gray-300 border-zinc-700 hover:border-white hover:text-white"
                  }`}
                >
                  {g.name}
                </Link>
              );
            })}
          </div>
        </div>

        {/* RÉSULTATS */}
        {movies.length > 0 ? (
          <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-5 gap-x-4 gap-y-10">
            {movies.map((movie) => (
              <div key={movie.id} className="relative z-0 hover:z-50">
                <MovieCard movie={movie} />
              </div>
            ))}
          </div>
        ) : (query || currentGenreId) ? (
          <div className="flex flex-col items-center justify-center mt-32 text-center space-y-4">
            <SearchX className="w-16 h-16 text-zinc-600" />
            <h2 className="text-xl font-medium text-gray-300">
              Aucun titre trouvé.
            </h2>
            <p className="text-sm text-zinc-500 max-w-md">
              Essayez d&apos;utiliser des mots-clés différents ou d&apos;élargir votre recherche en retirant des filtres.
            </p>
          </div>
        ) : null}
      </div>
    </main>
  );
}
