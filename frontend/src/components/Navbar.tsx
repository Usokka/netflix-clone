"use client";

import { useEffect, useState } from "react";
import { Search, Bell } from "lucide-react";

export default function Navbar() {
  const [isScrolled, setIsScrolled] = useState(false);

  useEffect(() => {
    const handleScroll = () => {
      if (window.scrollY > 0) {
        setIsScrolled(true);
      } else {
        setIsScrolled(false);
      }
    };

    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  return (
    <nav
      className={`fixed top-0 w-full h-16 flex items-center justify-between px-4 md:px-12 z-50 transition-colors duration-300 ${
        isScrolled ? "bg-[#141414]" : "bg-gradient-to-b from-black/80 to-transparent"
      }`}
    >
      {/* Côté Gauche : Logo et Liens */}
      <div className="flex items-center gap-8">
        <h1 className="text-red-600 font-extrabold text-xl md:text-3xl tracking-wider cursor-pointer transition hover:opacity-80">
          NETFLIX
        </h1>
        <ul className="hidden md:flex items-center gap-5 text-sm text-gray-300 font-medium">
          <li className="text-white font-semibold cursor-pointer transition hover:text-gray-300">Accueil</li>
          <li className="cursor-pointer transition hover:text-gray-300">Séries</li>
          <li className="cursor-pointer transition hover:text-gray-300">Films</li>
          <li className="cursor-pointer transition hover:text-gray-300">Ma Liste</li>
        </ul>
      </div>

      {/* Côté Droit : Actions et Profil */}
      <div className="flex items-center gap-6 text-white">
        <Search className="w-5 h-5 cursor-pointer hover:text-gray-300 transition" />
        <Bell className="w-5 h-5 cursor-pointer hover:text-gray-300 transition" />
        
        {/* Avatar Profil (Utilise les données fictives ou l'avatar par défaut) */}
        <div className="w-8 h-8 rounded overflow-hidden cursor-pointer border border-transparent hover:border-white transition">
          <img
            src="https://api.dicebear.com/7.x/bottts/svg?seed=Badis"
            alt="Profil"
            className="w-full h-full object-cover"
          />
        </div>
      </div>
    </nav>
  );
}