"use client";

import { useEffect, useRef, useState } from "react";
import { useRouter } from "next/navigation";
import { Search, Bell, LogOut, Users, CreditCard } from "lucide-react";
import { apiClient } from "@/lib/apiClient";
import { AuthResponse } from "@/types";
import { useProfile } from "@/context/ProfileContext";

export default function Navbar() {
  const router = useRouter();
  const [isScrolled, setIsScrolled] = useState(false);
  const [showDropdown, setShowDropdown] = useState(false);
  
  // NOUVEAU : États pour la recherche
  const [isSearchOpen, setIsSearchOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  
  const dropdownRef = useRef<HTMLDivElement>(null);
  const searchInputRef = useRef<HTMLInputElement>(null);
  
  const { activeProfile, setActiveProfile } = useProfile();

  useEffect(() => {
    const handleScroll = () => setIsScrolled(window.scrollY > 0);
    window.addEventListener("scroll", handleScroll);
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  useEffect(() => {
    const handleClickOutside = (e: MouseEvent) => {
      if (dropdownRef.current && !dropdownRef.current.contains(e.target as Node)) {
        setShowDropdown(false);
      }
    };
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const handleLogout = async () => {
    try {
      await apiClient.post<AuthResponse>("/auth/logout");
      setActiveProfile(null);
    } catch (err) {
      console.error("Échec de la déconnexion :", err);
    } finally {
      router.push("/login");
    }
  };

  const handleChangeProfile = () => {
    setActiveProfile(null);
    router.push("/profiles");
  };

  // NOUVEAU : Soumission du formulaire de recherche
  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    if (searchQuery.trim()) {
      router.push(`/search?q=${encodeURIComponent(searchQuery.trim())}`);
    }
  };

  return (
    <nav
      className={`fixed top-0 w-full h-16 flex items-center justify-between px-4 md:px-12 z-50 transition-colors duration-300 ${
        isScrolled ? "bg-[#141414]" : "bg-gradient-to-b from-black/80 to-transparent"
      }`}
    >
      <div className="flex items-center gap-8">
        <h1
          onClick={() => router.push("/")}
          className="text-red-600 font-extrabold text-xl md:text-3xl tracking-wider cursor-pointer transition hover:opacity-80 select-none"
        >
          NETFLIX
        </h1>
        <ul className="hidden md:flex items-center gap-5 text-sm text-gray-300 font-medium">
          <li onClick={() => router.push("/")} className="text-white font-semibold cursor-pointer transition hover:text-gray-300">Accueil</li>
          <li className="cursor-pointer transition hover:text-gray-300">Séries</li>
          <li className="cursor-pointer transition hover:text-gray-300">Films</li>
          <li className="cursor-pointer transition hover:text-gray-300">Ma Liste</li>
        </ul>
      </div>

      <div className="flex items-center gap-6 text-white">
        
        {/* NOUVEAU : Barre de recherche dynamique */}
        <div className="flex items-center">
          <form 
            onSubmit={handleSearch} 
            className={`flex items-center transition-all duration-300 ease-in-out ${
              isSearchOpen ? 'bg-black/80 border border-white p-1.5' : 'bg-transparent border-transparent p-1.5'
            }`}
          >
            <Search 
              className="w-5 h-5 cursor-pointer hover:text-gray-300 transition" 
              onClick={() => {
                setIsSearchOpen(!isSearchOpen);
                if (!isSearchOpen) setTimeout(() => searchInputRef.current?.focus(), 100);
              }} 
            />
            <input
              ref={searchInputRef}
              type="text"
              placeholder="Titres, mots-clés..."
              className={`bg-transparent border-none outline-none text-sm text-white transition-all duration-300 ease-in-out placeholder-gray-400 ${
                isSearchOpen ? 'w-32 md:w-56 ml-3 opacity-100' : 'w-0 opacity-0'
              }`}
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              onBlur={() => {
                // Se referme si on clique ailleurs et que c'est vide
                if (!searchQuery) setIsSearchOpen(false);
              }}
            />
          </form>
        </div>

        <Bell className="w-5 h-5 cursor-pointer hover:text-gray-300 transition" />

        <div className="relative" ref={dropdownRef}>
          <div
            onClick={() => setShowDropdown((prev) => !prev)}
            className="w-8 h-8 rounded overflow-hidden cursor-pointer border border-transparent hover:border-white transition"
          >
            <img
              src={activeProfile?.avatarUrl || `https://api.dicebear.com/7.x/bottts/svg?seed=fallback`}
              alt="Profil"
              className="w-full h-full object-cover bg-zinc-800"
            />
          </div>

          {showDropdown && (
            <div className="absolute right-0 mt-3 w-56 bg-black/95 border border-zinc-800 rounded shadow-md py-2 text-sm text-gray-200 z-50 backdrop-blur-sm">
              <div className="px-4 py-2 border-b border-zinc-800 text-xs text-gray-400 truncate">
                {activeProfile?.name || "Invité"}
              </div>
              <button onClick={handleChangeProfile} className="w-full text-left px-4 py-2.5 hover:bg-zinc-900 transition flex items-center gap-3 font-medium">
                <Users className="w-4 h-4" /> Changer de profil
              </button>
              <button onClick={() => { setShowDropdown(false); router.push("/account"); }} className="w-full text-left px-4 py-2.5 hover:bg-zinc-900 transition flex items-center gap-3 font-medium border-b border-zinc-800">
                <CreditCard className="w-4 h-4" /> Compte & Abonnements
              </button>
              <button onClick={handleLogout} className="w-full text-left px-4 py-2.5 hover:bg-zinc-900 transition flex items-center gap-3 text-red-500 font-medium">
                <LogOut className="w-4 h-4" /> Se déconnecter
              </button>
            </div>
          )}
        </div>
      </div>
    </nav>
  );
}