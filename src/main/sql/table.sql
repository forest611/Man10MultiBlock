create table recipes
(
	id int auto_increment,
	machine varchar(16) null,
	material text null,
	product text null,
	time int null,
	constraint recipes_pk
		primary key (id)
);