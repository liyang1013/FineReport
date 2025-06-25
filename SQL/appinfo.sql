create table appinfo
(
    version       varchar(10)  null comment '版本号',
    updateMessage varchar(50)  null comment '更新信息',
    downloadUrl   varchar(255) null comment '下载地址',
    forceUpdate   tinyint(1)   null comment '是否强制升级'
);