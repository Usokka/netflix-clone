import Navbar from "@/components/Navbar";
import MovieCard from "@/components/MovieCard";
import { Movie, Genre } from "@/types";
import { serverApiClient } from "@/lib/serverApiClient";
import { SearchX } from "lucide-react";
import Link from "next/link";

interface SearchPageProps {
  searchParams: Promise<{ q?: string; genre?: string }>;
}

export default async function SearchPage({ searchParams }: SearchPageProps) {
  const { q, genre } = await searchParams;
  const query = q || "";
  const currentGenreId = genre ? parseInt(genre, 10) : null;
  
  let movies: Movie[] = [];
  let allGenres: Genre[] = [];
  
  // On crée l'URL pour l'API backend
  let apiUrl = `/movies/search?`;
  if (query) apiUrl += `q=${encodeURIComponent(query)}&`;
  if (currentGenreId) apiUrl += `genre=${currentGenreId}`;

  try {
    // Exécution en parallèle : on récupère les films ET tous les genres dispos
    const [moviesRes, genresRes] = await Promise.allSettled([
      (query || currentGenreId) ? serverApiClient.get<Movie[]>(apiUrl) : Promise.resolve([]),
      serverApiClient.get<Genre[]>("/genres")
    ]);

    movies = moviesRes.status === 'fulfilled' ? moviesRes.value : [];
    allGenres = genresRes.status === 'fulfilled' ? genresRes.value : [];
  } catch (error) {
    console.error("Erreur lors de la recherche :", error);
  }

  // Fonction pour construire l'URL quand on clique sur un filtre de genre
  const buildGenreLink = (genreId: number) => {
    // Si on clique sur le genre déjà actif, on l'enlève (toggle)
    if (currentGenreId === genreId) {
      return `/search${query ? `?q=${query}` : ''}`;
    }
    // Sinon on l'ajoute
    return `/search?genre=${genreId}${query ? `&q=${query}` : ''}`;
  };

  return (
    <main className="min-h-screen bg-[#141414] text-white pt-24 pb-12 overflow-x-hidden">
      <Navbar />
      
      <div className="px-4 md:px-12 max-w-7xl mx-auto">
        
        {/* EN-TÊTE DE RECHERCHE */}
        <div className="mb-8">
          {query ? (
            <h1 className="text-xl md:text-2xl text-gray-400 font-medium mb-4">
              Résultats pour <span className="text-white font-bold">"{query}"</span>
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
                <Link key={g.id} href={buildGenreLink(g.id)}>
                  <div className={`px-4 py-1.5 rounded-full text-sm font-medium transition-all duration-300 border cursor-pointer ${
                    isActive 
                      ? "bg-white text-black border-white" 
                      : "bg-zinc-900/80 text-gray-300 border-zinc-700 hover:border-white hover:text-white"
                  }`}>
                    {g.name}
                  </div>
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
              Essayez d'utiliser des mots-clés différents ou d'élargir votre recherche en retirant des filtres.
            </p>
          </div>
        ) : null}
      </div>
    </main>
  );
}