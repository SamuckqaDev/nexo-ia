export function calculateAge(birthDate: string | null): number | null {
  if (!birthDate) return null;
  const birth: Date = new Date(`${birthDate}T00:00:00`);
  const today: Date = new Date();
  let age: number = today.getFullYear() - birth.getFullYear();
  const birthdayNotReached: boolean = today < new Date(today.getFullYear(), birth.getMonth(), birth.getDate());
  if (birthdayNotReached) age -= 1;
  return age;
}
