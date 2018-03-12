vpc_cidr = "10.0.0.0/16"

environment = "dev"

public_subnet_cidrs = ["10.0.0.0/24", "10.0.1.0/24"]

private_subnet_cidrs = ["10.0.50.0/24", "10.0.51.0/24"]

region = "eu-west-1"

availability_zones = ["eu-west-1a", "eu-west-1b"]

max_size = 2

min_size = 1

desired_capacity = 2

instance_type = "t2.micro"

ecs_aws_ami = "ami-95f8d2f3"

public_key_file = "~/.ssh/tcbuildreport-dev.key.pub"
