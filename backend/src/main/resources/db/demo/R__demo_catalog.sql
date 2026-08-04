INSERT INTO genres (name) VALUES ('Action') ON CONFLICT (name) DO NOTHING;
INSERT INTO genres (name) VALUES ('Sci-Fi') ON CONFLICT (name) DO NOTHING;
INSERT INTO genres (name) VALUES ('Drama') ON CONFLICT (name) DO NOTHING;

INSERT INTO movies (id, title, description, thumbnail_url, video_folder_url, duration_seconds, release_year, maturity_rating, language)
VALUES (
    'd4e5f6a7-b8c9-d0e1-2f3a-4b5c6d7e8f9a',
    'Sintel',
    'The story of a lonely young woman who befriends a baby dragon named Scales.',
    'https://upload.wikimedia.org/wikipedia/commons/8/8f/Sintel_poster.jpg',
    'sintel',
    900,
    2010,
    'PG-13',
    'en'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO movies (id, title, description, thumbnail_url, video_folder_url, duration_seconds, release_year, maturity_rating, language)
VALUES (
    'e5f6a7b8-c9d0-e1f2-3a4b-5c6d7e8f9a0b',
    'Tears of Steel',
    'A sci-fi track set in dystopian Amsterdam with giant robots.',
    'https://upload.wikimedia.org/wikipedia/commons/6/69/Tears_of_Steel_poster.jpg',
    'tears-of-steel',
    734,
    2012,
    'PG-13',
    'en'
) ON CONFLICT (id) DO NOTHING;

INSERT INTO movie_genres (movie_id, genre_id)
SELECT 'd4e5f6a7-b8c9-d0e1-2f3a-4b5c6d7e8f9a', id FROM genres WHERE name = 'Action'
ON CONFLICT DO NOTHING;
INSERT INTO movie_genres (movie_id, genre_id)
SELECT 'd4e5f6a7-b8c9-d0e1-2f3a-4b5c6d7e8f9a', id FROM genres WHERE name = 'Drama'
ON CONFLICT DO NOTHING;
INSERT INTO movie_genres (movie_id, genre_id)
SELECT 'e5f6a7b8-c9d0-e1f2-3a4b-5c6d7e8f9a0b', id FROM genres WHERE name = 'Sci-Fi'
ON CONFLICT DO NOTHING;
