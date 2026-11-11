CREATE TABLE IF NOT EXISTS settings(setting_key VARCHAR(100) PRIMARY KEY,setting_value TEXT NOT NULL) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
CREATE TABLE IF NOT EXISTS links(id INT AUTO_INCREMENT PRIMARY KEY,label VARCHAR(120) NOT NULL,url VARCHAR(500) NOT NULL,icon VARCHAR(40) NOT NULL DEFAULT 'language',sort_order INT NOT NULL DEFAULT 0,is_active TINYINT(1) NOT NULL DEFAULT 1) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT INTO settings(setting_key,setting_value) VALUES
('station_name','La Grande de Huétamo'),('frequency','106.9 FM'),('website_url',''),('headline','La Grande de Huétamo'),('eyebrow','LA GRANDE'),('subtitle','Radio La Grande de Huétamo'),('status_text','Listo para escuchar'),('stream_url',''),('logo_url',''),('background_url',''),('facebook_url','https://facebook.com/'),('whatsapp_number',''),('whatsapp_message','Hola, quiero comunicarme con La Grande de Huétamo.'),('primary_color','#D71920'),('accent_color','#F5C400'),('chat_enabled','1')
ON DUPLICATE KEY UPDATE setting_key=VALUES(setting_key);
INSERT INTO links(label,url,icon,sort_order,is_active) SELECT 'La Grande de Huétamo','https://example.com','language',1,1 WHERE NOT EXISTS(SELECT 1 FROM links);
