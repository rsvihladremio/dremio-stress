package main

import (
	"fmt"
	"os"

	"github.com/rsvihladremio/dremio-stress/cmd"
)

func main() {
	args, err := cmd.ParseArgs()
	if err != nil {
		fmt.Println(args)
		os.Exit(1)
	}
	if err := cmd.Execute(args); err != nil {
		fmt.Println(args)
		os.Exit(1)
	}
}
