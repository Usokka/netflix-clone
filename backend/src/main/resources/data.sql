INSERT INTO users (id, email, password_hash, role, created_at)
VALUES (
    'a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d',
    'test@netflix.com',
    '$2a$10$X5vH8R1qW/w0XvB4Pz7uO.Y5v6J7K8L9M0N1O2P3Q4R5S6T7U8V9W',
    'ROLE_USER',
    CURRENT_TIMESTAMP
) ON CONFLICT (email) DO NOTHING;

INSERT INTO subscriptions (id, user_id, plan, started_at, expires_at, is_active)
VALUES (
    'b2c3d4e5-f6a7-b8c9-d0e1-2f3a4b5c6d7e',
    'a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d',
    'PREMIUM',
    CURRENT_DATE,
    CURRENT_DATE + INTERVAL '1 year',
    TRUE
) ON CONFLICT (id) DO NOTHING;

INSERT INTO profiles (id, user_id, name, avatar_url)
VALUES (
    'c3d4e5f6-a7b8-c9d0-e1f2-3a4b5c6d7e8f',
    'a1b2c3d4-e5f6-7a8b-9c0d-1e2f3a4b5c6d',
    'Badis',
    'https://api.dicebear.com/7.x/bottts/svg?seed=Badis'
) ON CONFLICT (id) DO NOTHING;

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
VALUES ('d4e5f6a7-b8c9-d0e1-2f3a-4b5c6d7e8f9a', 1) ON CONFLICT DO NOTHING;
INSERT INTO movie_genres (movie_id, genre_id)
VALUES ('d4e5f6a7-b8c9-d0e1-2f3a-4b5c6d7e8f9a', 3) ON CONFLICT DO NOTHING;
INSERT INTO movie_genres (movie_id, genre_id)
VALUES ('e5f6a7b8-c9d0-e1f2-3a4b-5c6d7e8f9a0b', 2) ON CONFLICT DO NOTHING;