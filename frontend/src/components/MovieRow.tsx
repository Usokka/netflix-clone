import {
  Carousel,
  CarouselContent,
  CarouselItem,
  CarouselNext,
  CarouselPrevious,
} from "@/components/ui/carousel";
import MovieCard from "./MovieCard";
import { Movie } from "@/types";

interface MovieRowProps {
  title: string;
  movies: Movie[];
}

export default function MovieRow({ title, movies }: MovieRowProps) {
  if (!movies || movies.length === 0) return null;

  return (
    <div className="space-y-2 px-4 md:px-12 my-8">
      <h2 className="text-sm md:text-2xl font-semibold text-[#e5e5e5] transition-colors duration-200 hover:text-white cursor-pointer inline-block">
        {title}
      </h2>

      <Carousel
        opts={{
          align: "start",
          loop: true,
        }}
        className="w-full relative group"
      >
        <CarouselContent className="-ml-2 md:-ml-4">
          {movies.map((movie) => (
            <CarouselItem 
              key={movie.id} 
              className="pl-2 md:pl-4 basis-1/2 sm:basis-1/3 md:basis-1/4 lg:basis-1/5 xl:basis-1/6"
            >
              <MovieCard movie={movie} />
            </CarouselItem>
          ))}
        </CarouselContent>

        <CarouselPrevious className="absolute left-2 top-1/2 -translate-y-1/2 bg-black/50 text-white border-none h-12 w-8 rounded-md opacity-0 group-hover:opacity-100 transition-opacity hover:bg-black/80 disabled:hidden" />
        <CarouselNext className="absolute right-2 top-1/2 -translate-y-1/2 bg-black/50 text-white border-none h-12 w-8 rounded-md opacity-0 group-hover:opacity-100 transition-opacity hover:bg-black/80 disabled:hidden" />
      </Carousel>
    </div>
  );
}