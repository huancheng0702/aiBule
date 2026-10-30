<template>
	<view>
		<web-view :src="src"></web-view>
	</view>
</template>

<script>
	export default {
		data() {
			return {
				src:'',
				id:''
			}
		},
		onLoad(options) {
			this.src = options.src
			this.id = options.id
			console.log(this.id)
			if(this.id == 5){
				uni.setNavigationBarTitle({
					title:'隐私政策'
				})
			}
			if(this.id == 1){
				uni.setNavigationBarTitle({
					title:'用户协议'
				})
			}
		},
		methods: {
			
		}
	}
</script>

<style>
page{
	background-color: #fff;
}
</style>