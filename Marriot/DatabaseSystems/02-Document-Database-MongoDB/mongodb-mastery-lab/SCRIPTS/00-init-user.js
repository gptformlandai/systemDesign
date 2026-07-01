const databaseName = 'mongodb_mastery';
const appUser = 'app';
const appPassword = 'app_password';

const database = db.getSiblingDB(databaseName);

if (!database.getUser(appUser)) {
  database.createUser({
    user: appUser,
    pwd: appPassword,
    roles: [
      { role: 'readWrite', db: databaseName },
      { role: 'dbAdmin', db: databaseName }
    ]
  });
  print(`Created user ${appUser} for ${databaseName}`);
} else {
  print(`User ${appUser} already exists`);
}
