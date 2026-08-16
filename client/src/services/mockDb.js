// Mock Database Service (In-Memory Implementation)
// This avoids native dependency requirements (like AsyncStorage) for a smoother Expo Go demo.

let USERS_DB = [];

let LOGGED_IN_USER = null;

export async function getUsers() {
  return USERS_DB;
}

export async function findUserByPhone(phone) {
  const cleanPhone = phone.replace(/\D/g, '');
  return USERS_DB.find(u => u.phone.replace(/\D/g, '') === cleanPhone);
}

export async function registerUser(phone, email, password) {
  const cleanPhone = phone.replace(/\D/g, '');
  
  // Check if user already exists
  const exists = USERS_DB.some(u => u.phone.replace(/\D/g, '') === cleanPhone);
  if (exists) {
    throw new Error('User with this phone number already exists.');
  }

  const newUser = {
    phone: cleanPhone,
    email: email.trim().toLowerCase(),
    password: password, // In a real app, hash this
    createdAt: new Date().toISOString(),
  };

  USERS_DB.push(newUser);
  return newUser;
}

export async function getLoggedInUser() {
  return LOGGED_IN_USER;
}

export async function setLoggedInUser(user) {
  LOGGED_IN_USER = user;
}

export async function clearDatabase() {
  USERS_DB = [];
  LOGGED_IN_USER = null;
}
