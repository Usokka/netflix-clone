package com.netflixclone.api.repositories;

import com.netflixclone.api.models.Movie;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MovieRepository extends JpaRepository<Movie, UUID> {
    
    @Query("SELECT m FROM Movie m JOIN m.genres g WHERE g.id = :genreId")
    List<Movie> findByGenreId(@Param("genreId") Integer genreId);

    @Query("""
            SELECT m
            FROM Movie m
            LEFT JOIN m.watchHistoryEntries history
            GROUP BY m
            ORDER BY COUNT(history) DESC, MAX(history.watchedAt) DESC, m.releaseYear DESC
            """)
    List<Movie> findTrending(Pageable pageable);

    @Query("""
            SELECT DISTINCT m
            FROM Movie m
            LEFT JOIN m.genres genre
            WHERE (:query IS NULL
                    OR LOWER(m.title) LIKE LOWER(CONCAT('%', :query, '%'))
                    OR LOWER(m.description) LIKE LOWER(CONCAT('%', :query, '%')))
              AND (:genreId IS NULL OR genre.id = :genreId)
            ORDER BY m.title ASC
            """)
    List<Movie> search(
            @Param("query") String query,
            @Param("genreId") Integer genreId,
            Pageable pageable
    );

    Optional<Movie> findByVideoFolderUrl(String videoFolderUrl);
}
