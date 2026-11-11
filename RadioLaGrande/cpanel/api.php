<?php
declare(strict_types=1);
require_once __DIR__.'/config.php';
header('Content-Type: application/json; charset=utf-8');header('Cache-Control: no-store');
try {
 $links=db()->query('SELECT id,label,url,icon,sort_order FROM links WHERE is_active=1 ORDER BY sort_order,id')->fetchAll();
 $keys=['station_name','frequency','headline','eyebrow','subtitle','status_text','stream_url','logo_url','background_url','facebook_url','website_url','whatsapp_number','whatsapp_message','primary_color','accent_color','chat_enabled'];
 $settings=[];foreach($keys as $key)$settings[$key]=setting($key);
 $base=rtrim(dirname((isset($_SERVER['HTTPS'])&&$_SERVER['HTTPS']!=='off'?'https':'http').'://'.$_SERVER['HTTP_HOST'].$_SERVER['SCRIPT_NAME']),'/').'/';
 foreach(['logo_url','background_url'] as $k)if($settings[$k]!==''&&!preg_match('~^https?://~i',$settings[$k]))$settings[$k]=$base.ltrim($settings[$k],'/');
 echo json_encode(['ok'=>true,'settings'=>$settings,'links'=>$links],JSON_UNESCAPED_UNICODE|JSON_UNESCAPED_SLASHES);
} catch(Throwable $e){http_response_code(500);echo json_encode(['ok'=>false,'error'=>'No se pudo cargar la configuración']);}
