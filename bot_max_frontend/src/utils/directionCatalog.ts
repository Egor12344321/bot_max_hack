import { getDirections } from "@/api/planningApi";
import type { StudyDirection } from "@/api/types/planning";

export async function resolveDirections(
  ids: string[],
): Promise<StudyDirection[]> {
  const found = new Map<string, StudyDirection>();
  let offset = 0;
  while (found.size < ids.length) {
    const page = await getDirections("", offset);
    for (const item of page.items)
      if (ids.includes(item.id)) found.set(item.id, item);
    offset += page.items.length;
    if (!page.items.length || offset >= page.total) break;
  }
  return ids.map(
    (id) => found.get(id) ?? { id, code: "", name: `Направление ${id}` },
  );
}
