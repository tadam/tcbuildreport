## How to deploy in AWS

AWS ECS is used for infrastructure. Terraform is used for IaC description of it.

Original [terraform-ecs](https://github.com/arminc/terraform-ecs) is quite good starting point for ECS cluster provisioning. The author doesn't mean to make `terraform-ecs` a general-purpose module, so this repo has benn forked into [tadam/terraform-ecs](https://github.com/tadam/terraform-ecs/tree/tcbuildreport) to accomodate small changes necessary for `tcbuildreport` (see `tcbuildreport` branch). This repo is added as a git submodule in `tcbuildreport` just for convenience.

`tcbuildreport` containers are stored publicly in Docker Hub.

### Deployment

Install [Terraform](https://www.terraform.io/).

[Configure AWS CLI](https://docs.aws.amazon.com/cli/latest/userguide/cli-chap-getting-started.html) if needed. If you don't want to use credentials from `~/.aws/credentials` then see other supported options in [Terraform AWS Provider](https://www.terraform.io/docs/providers/aws/).

Change some variables in `ecs.tfvars` as you wish. In particular, `environment` variable is used as name prefix for all created resources. So if you have already some resources in AWS, make this prefix unique.

Also check `public_key_file` variable. You can generate key pair using `generate_keys.sh` if needed. You can SSH to instances in created ECS cluster using this key pair. The easiest way is to create a bastion host in public subnet of created VPC and change security policy of ECS instances so that its SSH port is open for bastion host on them. This setup uses Amazon's ECS AMI images, so you'll need to SSH as `ec2-user`.

In `td-tcbuildreport-backend.json` you'll find ECS a stripped task definition for `tcbuildreport-backend`. In `tcbuildreport.tf` we create corresponding task definition and ECS service.


```sh
cd terraform-ecs

# first time only
./tf init

./tf plan

# if you like the plan, then apply it
./tf apply
```

Do not delete created `terraform.state`!

Run `terraform destroy` to destroy all created infrastructure.