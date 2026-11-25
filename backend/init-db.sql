CREATE DATABASE IF NOT EXISTS db_usuarios CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS db_activos CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE DATABASE IF NOT EXISTS db_protocolos CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON db_usuarios.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON db_activos.* TO 'root'@'%';
GRANT ALL PRIVILEGES ON db_protocolos.* TO 'root'@'%';
FLUSH PRIVILEGES;
