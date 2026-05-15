export function IsekaiTruck({ label = "truck-kun" }: { label?: string }) {
  return (
    <span className="isekai-truck" aria-label={label}>
      {/* Image asset: place at frontend/public/anime-isekai/truck-kun.webp (see plan for prompt). */}
      <img src="/anime-isekai/truck-kun.webp" alt="" />
    </span>
  );
}
