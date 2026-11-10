<?php
declare(strict_types=1);
const DB_HOST = 'localhost';
const DB_NAME = 'CAMBIAR_BASE_DATOS';
const DB_USER = 'CAMBIAR_USUARIO';
const DB_PASS = 'CAMBIAR_PASSWORD';
const ADMIN_USER = 'admin';
const ADMIN_PASSWORD_HASH = 'REEMPLAZAR_POR_HASH_GENERADO';
const APP_NAME = 'La Grande de Huétamo';
const UPLOAD_DIR = __DIR__ . '/uploads/';
const UPLOAD_URL = 'uploads/';
function db(): PDO {
 static $pdo=null; if($pdo instanceof PDO)return $pdo;
 $pdo=new PDO('mysql:host='.DB_HOST.';dbname='.DB_NAME.';charset=utf8mb4',DB_USER,DB_PASS,
 [PDO::ATTR_ERRMODE=>PDO::ERRMODE_EXCEPTION,PDO::ATTR_DEFAULT_FETCH_MODE=>PDO::FETCH_ASSOC]); return $pdo;
}
function setting(string $key,string $default=''):string {
 $q=db()->prepare('SELECT setting_value FROM settings WHERE setting_key=? LIMIT 1');$q->execute([$key]);$v=$q->fetchColumn();return $v===false?$default:(string)$v;
}
function save_setting(string $key,string $value):void {
 $q=db()->prepare('INSERT INTO settings(setting_key,setting_value) VALUES(?,?) ON DUPLICATE KEY UPDATE setting_value=VALUES(setting_value)');$q->execute([$key,$value]);
}
function h(string $s):string{return htmlspecialchars($s,ENT_QUOTES,'UTF-8');}
function require_admin():void{session_start();if(empty($_SESSION['radio_admin'])){header('Location: admin.php');exit;}}
