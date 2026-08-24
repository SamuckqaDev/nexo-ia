import type { z } from "zod";
import type { personalMemorySchema } from "../schemas/personalMemorySchemas";

export type PersonalMemory = z.infer<typeof personalMemorySchema>;
