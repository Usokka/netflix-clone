#include "utils.hpp"
#include <algorithm>
#include <fstream>   
#include <jwt-cpp/jwt.h> 

bool is_path_safe(const std::string &path) {
  if (path.find("..") != std::string::npos) {
    return false;
  }
  if (path.find('\\') != std::string::npos ||
      path.find('\0') != std::string::npos) {
    return false;
  }
  return true;
}

std::string get_content_type(const std::string &path) {
  if (path.rfind(".m3u8") != std::string::npos) {
    return "application/x-mpegURL";
  }
  if (path.rfind(".ts") != std::string::npos) {
    return "video/MP2T";
  }
  return "application/octet-stream";
}


std::string extract_ticket(const std::string& url) {
    size_t pos = url.find("ticket=");
    if (pos == std::string::npos) {
        return "";
    }
    return url.substr(pos + 7);
}

bool verify_streaming_ticket(const std::string& ticket, const std::string& client_ip, const std::string& requested_movie_id) {
    if (ticket.empty()) return false;

    try {
      
       std::ifstream pub_file("/etc/secrets/public.pem");

        if (!pub_file.is_open()) {
            // Repli local A : Si exécuté depuis la racine du dossier 'streaming'
            pub_file.open("../infra/secrets/public.pem");
            
            if (!pub_file.is_open()) {
                // Repli local B : Si exécuté depuis l'intérieur du dossier 'streaming/build'
                pub_file.open("../../infra/secrets/public.pem");
            }
        }

        if (!pub_file.is_open()) {
            std::cerr << "[ERROR] Impossible de charger public.pem ! Vérifie l'existence de infra/secrets/public.pem" << std::endl;
            return false;
        }
        
        std::stringstream ss;
        ss << pub_file.rdbuf();
        std::string public_key = ss.str();

      
        auto decoded = jwt::decode(ticket);
        auto verifier = jwt::verify()
            .allow_algorithm(jwt::algorithm::rs256(public_key, "", "", ""))
            .with_issuer("netflix-backend");

      
        verifier.verify(decoded);

      
        if (decoded.has_payload_claim("movieId")) {
            std::string token_movie_id = decoded.get_payload_claim("movieId").as_string();
            if (token_movie_id != requested_movie_id) {
                std::cerr << "[SECURITY] Incohérence de MovieId ! Attendu: " << requested_movie_id << ", Reçu: " << token_movie_id << std::endl;
                return false;
            }
        } else {
            return false;
        }

        if (decoded.has_payload_claim("ip")) {
            std::string token_ip = decoded.get_payload_claim("ip").as_string();
          
            if (token_ip != client_ip && client_ip != "127.0.0.1") {
                std::cout << "[INFO] IP mismatch (Changement de réseau/Proxy) - Token: " << token_ip << ", Client: " << client_ip << std::endl;
            }
        }

        return true;

    } catch (const std::exception& e) {
        std::cerr << "[SECURITY] Échec de la validation cryptographique : " << e.what() << std::endl;
        return false;
    }
}